#!/bin/bash

set -e  # Stop on first error

# -------------------- Configuration --------------------
FUNCTION_NAME=unsubscribe
HANDLER=com.amazonaws.project.UnsubscribeLambda::handleRequest
JAR_PATH=target/Music-project-1.0-SNAPSHOT.jar
RUNTIME=java17

API_NAME=music-api
RESOURCE_PATH=unsubscribe
STAGE_NAME=dev
LAMBDA_URI="arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/arn:aws:lambda:us-east-1:000000000000:function:$FUNCTION_NAME/invocations"
STATEMENT_ID=apigateway-unsubscribe-permission

# -------------------- Step 1: Create or Update Lambda --------------------
echo "📦 Building and deploying Lambda function: $FUNCTION_NAME"
mvn clean package || { echo "❌ Build failed"; exit 1; }

echo "🚀 Deploying Lambda..."
awslocal lambda create-function \
  --function-name $FUNCTION_NAME \
  --runtime $RUNTIME \
  --handler $HANDLER \
  --role arn:aws:iam::000000000000:role/lambda-role \
  --zip-file fileb://$JAR_PATH 2>/dev/null ||
awslocal lambda update-function-code \
  --function-name $FUNCTION_NAME \
  --zip-file fileb://$JAR_PATH

# -------------------- Step 2: Setup API Gateway --------------------
echo "🌐 Setting up API Gateway for $RESOURCE_PATH..."

API_ID=$(awslocal apigateway get-rest-apis --query "items[?name=='$API_NAME'].id" --output text)

if [ -z "$API_ID" ]; then
  API_ID=$(awslocal apigateway create-rest-api --name "$API_NAME" --query 'id' --output text)
  echo "✅ Created new API: $API_ID"
else
  echo "🔁 Reusing existing API: $API_ID"
fi

PARENT_ID=$(awslocal apigateway get-resources --rest-api-id "$API_ID" \
  --query "items[?path=='/'].id" --output text)

RESOURCE_ID=$(awslocal apigateway get-resources --rest-api-id "$API_ID" \
  --query "items[?path=='/$RESOURCE_PATH'].id" --output text)

if [ -z "$RESOURCE_ID" ]; then
  RESOURCE_ID=$(awslocal apigateway create-resource \
    --rest-api-id "$API_ID" \
    --parent-id "$PARENT_ID" \
    --path-part "$RESOURCE_PATH" \
    --query 'id' --output text)
  echo "📘 Created /$RESOURCE_PATH resource with ID: $RESOURCE_ID"
else
  echo "🔁 Reusing resource /$RESOURCE_PATH: $RESOURCE_ID"
fi

# -------------------- Step 3: Add POST Integration --------------------
echo "🔧 Adding POST method integration..."
awslocal apigateway put-method \
  --rest-api-id "$API_ID" \
  --resource-id "$RESOURCE_ID" \
  --http-method POST \
  --authorization-type "NONE"

awslocal apigateway put-integration \
  --rest-api-id "$API_ID" \
  --resource-id "$RESOURCE_ID" \
  --http-method POST \
  --type AWS_PROXY \
  --integration-http-method POST \
  --uri "$LAMBDA_URI"

# -------------------- Step 4: Enable CORS (OPTIONS method) --------------------
echo "🛡️ Adding CORS support to /$RESOURCE_PATH"
awslocal apigateway put-method \
  --rest-api-id "$API_ID" \
  --resource-id "$RESOURCE_ID" \
  --http-method OPTIONS \
  --authorization-type "NONE"

awslocal apigateway put-method-response \
  --rest-api-id "$API_ID" \
  --resource-id "$RESOURCE_ID" \
  --http-method OPTIONS \
  --status-code 200 \
  --response-parameters '{
    "method.response.header.Access-Control-Allow-Headers": true,
    "method.response.header.Access-Control-Allow-Methods": true,
    "method.response.header.Access-Control-Allow-Origin": true
  }' \
  --response-models '{"application/json": "Empty"}'

awslocal apigateway put-integration \
  --rest-api-id "$API_ID" \
  --resource-id "$RESOURCE_ID" \
  --http-method OPTIONS \
  --type MOCK \
  --request-templates '{"application/json":"{\"statusCode\": 200}"}'

awslocal apigateway put-integration-response \
  --rest-api-id "$API_ID" \
  --resource-id "$RESOURCE_ID" \
  --http-method OPTIONS \
  --status-code 200 \
  --response-parameters '{
    "method.response.header.Access-Control-Allow-Headers": "'"'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'"'",
    "method.response.header.Access-Control-Allow-Methods": "'"'POST,OPTIONS'"'",
    "method.response.header.Access-Control-Allow-Origin": "'"'*'"'"
  }' \
  --response-templates '{"application/json": ""}'

# -------------------- Step 5: Grant API Gateway Invoke Permission --------------------
echo "🔐 Adding permission for API Gateway to invoke Lambda..."
PERMISSION_EXISTS=$(awslocal lambda get-policy --function-name "$FUNCTION_NAME" 2>/dev/null | grep "$STATEMENT_ID" || true)
if [ -z "$PERMISSION_EXISTS" ]; then
  awslocal lambda add-permission \
    --function-name "$FUNCTION_NAME" \
    --statement-id "$STATEMENT_ID" \
    --action lambda:InvokeFunction \
    --principal apigateway.amazonaws.com \
    --source-arn "arn:aws:execute-api:us-east-1:000000000000:$API_ID/*/POST/$RESOURCE_PATH"
else
  echo "✅ Permission already exists."
fi

# -------------------- Step 6: Deploy --------------------
echo "🚀 Deploying the API..."
awslocal apigateway create-deployment \
  --rest-api-id "$API_ID" \
  --stage-name "$STAGE_NAME"

# -------------------- Final Output --------------------
echo ""
echo "✅ Unsubscribe endpoint is ready:"
echo "👉 http://localhost:4566/restapis/$API_ID/$STAGE_NAME/_user_request_/$RESOURCE_PATH"
