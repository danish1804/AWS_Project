#!/bin/bash

# -------------------- Step 1: Build JAR --------------------
echo "🛠️ Building the Lambda project..."
mvn clean package || { echo "❌ Build failed"; exit 1; }

# Path to the built jar
JAR_PATH=target/Music-project-1.0-SNAPSHOT.jar

# Lambda function details
FUNCTION_NAME=loginUser
HANDLER=com.amazonaws.project.LoginUserLambda::handleRequest
RUNTIME=java17

# -------------------- Step 2: Deploy Lambda --------------------
echo "🚀 Creating or updating Lambda function: $FUNCTION_NAME"

# Try to create the function, if it exists then update
awslocal lambda create-function \
  --function-name $FUNCTION_NAME \
  --runtime $RUNTIME \
  --handler $HANDLER \
  --role arn:aws:iam::000000000000:role/lambda-role \
  --zip-file fileb://$JAR_PATH 2>/dev/null ||
awslocal lambda update-function-code \
  --function-name $FUNCTION_NAME \
  --zip-file fileb://$JAR_PATH

# -------------------- Step 3: Setup API Gateway --------------------
echo "🌐 Setting up API Gateway..."

API_NAME=music-api
STAGE_NAME=dev
RESOURCE_PATH=login

# Get or create API
API_ID=$(awslocal apigateway get-rest-apis --query "items[?name=='$API_NAME'].id" --output text)

if [ -z "$API_ID" ]; then
  API_ID=$(awslocal apigateway create-rest-api --name "$API_NAME" --query 'id' --output text)
  echo "📦 Created API Gateway with ID: $API_ID"
else
  echo "🔄 Reusing existing API Gateway with ID: $API_ID"
fi

# Get root resource
PARENT_ID=$(awslocal apigateway get-resources --rest-api-id "$API_ID" \
  --query "items[?path=='/'].id" --output text)

# Create /login resource if it doesn't exist
RESOURCE_ID=$(awslocal apigateway get-resources --rest-api-id "$API_ID" \
  --query "items[?path=='/$RESOURCE_PATH'].id" --output text)

if [ -z "$RESOURCE_ID" ]; then
  RESOURCE_ID=$(awslocal apigateway create-resource \
    --rest-api-id "$API_ID" \
    --parent-id "$PARENT_ID" \
    --path-part "$RESOURCE_PATH" \
    --query 'id' --output text)
  echo "📘 Created resource /$RESOURCE_PATH with ID: $RESOURCE_ID"
fi

# Add POST method
awslocal apigateway put-method \
  --rest-api-id "$API_ID" \
  --resource-id "$RESOURCE_ID" \
  --http-method POST \
  --authorization-type "NONE"

# Integrate POST /login with Lambda
awslocal apigateway put-integration \
  --rest-api-id "$API_ID" \
  --resource-id "$RESOURCE_ID" \
  --http-method POST \
  --type AWS_PROXY \
  --integration-http-method POST \
  --uri arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/arn:aws:lambda:us-east-1:000000000000:function:$FUNCTION_NAME/invocations

# -------------------- Step 4: Add CORS (OPTIONS method) --------------------
echo "🛡️ Adding CORS OPTIONS method for /$RESOURCE_PATH..."
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
    "method.response.header.Access-Control-Allow-Origin": true,
    "method.response.header.Access-Control-Allow-Methods": true,
    "method.response.header.Access-Control-Allow-Headers": true
  }'

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
     "method.response.header.Access-Control-Allow-Headers":"'\''Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'\''",
     "method.response.header.Access-Control-Allow-Methods":"'\''POST,OPTIONS'\''",
     "method.response.header.Access-Control-Allow-Origin":"'\''*'\''"
   }'

# -------------------- Step 5: Deploy API --------------------
awslocal apigateway create-deployment \
  --rest-api-id "$API_ID" \
  --stage-name "$STAGE_NAME"

# -------------------- Final Output --------------------
echo ""
echo "✅ Login endpoint ready at:"
echo "👉 http://localhost:4566/restapis/$API_ID/$STAGE_NAME/_user_request_/$RESOURCE_PATH"
