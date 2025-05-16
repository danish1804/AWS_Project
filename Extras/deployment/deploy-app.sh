#!/bin/bash
AWLOCAL=/usr/local/bin/awslocal
#export PATH="/usr/local/bin:$PATH" # ✅ add this


# -------------------- CONFIG --------------------
JAR_PATH=target/Music-project-1.0-SNAPSHOT.jar
FUNCTION_NAME=MusicAppHandler
HANDLER=com.amazonaws.project.handler.AppLambdaHandler::handleRequest
RUNTIME=java17
ROLE_ARN=arn:aws:iam::000000000000:role/lambda-role
API_NAME=music-api
STAGE_NAME=dev
ENDPOINTS=("login" "register" "subscribe" "unsubscribe" "getSubscriptions" "searchMusic")
REGION=us-east-1

# -------------------- Step 1: Build JAR --------------------
echo "🛠️ Building the Lambda project..."
mvn clean package || { echo "❌ Build failed"; exit 1; }

# -------------------- Step 2: Deploy Lambda --------------------
echo "🚀 Creating or updating unified Lambda function: $FUNCTION_NAME"

$AWLOCAL lambda create-function \
  --function-name $FUNCTION_NAME \
  --runtime $RUNTIME \
  --handler $HANDLER \
  --role $ROLE_ARN \
  --zip-file fileb://$JAR_PATH 2>/dev/null ||
$AWLOCAL lambda update-function-code \
  --function-name $FUNCTION_NAME \
  --zip-file fileb://$JAR_PATH

$AWLOCAL lambda get-function-configuration --function-name $FUNCTION_NAME


# -------------------- Step 3: Setup API Gateway --------------------
echo "🌐 Setting up API Gateway..."

API_ID=$($AWLOCAL apigateway get-rest-apis --query "items[?name=='$API_NAME'].id" --output text)

if [ -z "$API_ID" ]; then
  API_ID=$($AWLOCAL apigateway create-rest-api --name "$API_NAME" --query 'id' --output text)
  echo "📦 Created API Gateway with ID: $API_ID"
else
  echo "🔄 Reusing existing API Gateway with ID: $API_ID"
fi

PARENT_ID=$($AWLOCAL apigateway get-resources --rest-api-id "$API_ID" \
  --query "items[?path=='/'].id" --output text)

# -------------------- Step 4: Create Each Resource --------------------
for PATH in "${ENDPOINTS[@]}"; do
  echo "PATH inside loop: $(echo $PATH)" # For debugging
  echo "🔧 Setting up /$PATH"

  RESOURCE_ID=$($AWLOCAL apigateway get-resources --rest-api-id "$API_ID" \
    --query "items[?path=='/$PATH'].id" --output text)

  if [ -z "$RESOURCE_ID" ]; then
    RESOURCE_ID=$($AWLOCAL apigateway create-resource \
      --rest-api-id "$API_ID" \
      --parent-id "$PARENT_ID" \
      --path-part "$PATH" \
      --query 'id' --output text)
    echo "📘 Created resource /$PATH with ID: $RESOURCE_ID"
  fi

  # Add POST method
  $AWLOCAL apigateway put-method \
    --rest-api-id "$API_ID" \
    --resource-id "$RESOURCE_ID" \
    --http-method POST \
    --authorization-type "NONE"

  # Integrate with the unified Lambda
  $AWLOCAL apigateway put-integration \
    --rest-api-id "$API_ID" \
    --resource-id "$RESOURCE_ID" \
    --http-method POST \
    --type AWS_PROXY \
    --integration-http-method POST \
    --uri arn:aws:apigateway:$REGION:lambda:path/2015-03-31/functions/arn:aws:lambda:$REGION:000000000000:function:$FUNCTION_NAME/invocations

  # Add OPTIONS method for CORS
  $AWLOCAL apigateway put-method \
    --rest-api-id "$API_ID" \
    --resource-id "$RESOURCE_ID" \
    --http-method OPTIONS \
    --authorization-type "NONE"

  $AWLOCAL apigateway put-method-response \
    --rest-api-id "$API_ID" \
    --resource-id "$RESOURCE_ID" \
    --http-method OPTIONS \
    --status-code 200 \
    --response-parameters '{
      "method.response.header.Access-Control-Allow-Origin": true,
      "method.response.header.Access-Control-Allow-Methods": true,
      "method.response.header.Access-Control-Allow-Headers": true
    }'

  $AWLOCAL apigateway put-integration \
    --rest-api-id "$API_ID" \
    --resource-id "$RESOURCE_ID" \
    --http-method OPTIONS \
    --type MOCK \
    --request-templates '{"application/json":"{\"statusCode\": 200}"}'

  $AWLOCAL apigateway put-integration-response \
     --rest-api-id "$API_ID" \
     --resource-id "$RESOURCE_ID" \
     --http-method OPTIONS \
     --status-code 200 \
     --response-parameters '{
       "method.response.header.Access-Control-Allow-Headers":"'\''Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'\''",
       "method.response.header.Access-Control-Allow-Methods":"'\''GET,POST,OPTIONS'\''",
       "method.response.header.Access-Control-Allow-Origin":"'\''*'\''"
     }'
done

# -------------------- Step 5: Deploy the API --------------------
echo "📦 Deploying API Gateway to stage: $STAGE_NAME"
$AWLOCAL apigateway create-deployment \
  --rest-api-id "$API_ID" \
  --stage-name "$STAGE_NAME"

# -------------------- Final Output --------------------
echo ""
echo "✅ All endpoints are ready:"
for PATH in "${ENDPOINTS[@]}"; do
  echo "👉 http://localhost:4566/restapis/$API_ID/$STAGE_NAME/_user_request_/$PATH"
done




# -------------------- CONFIG --------------------

OUTPUT_FILE=config.json


## -------------------- Step 1: Get API ID from LocalStack --------------------
#API_ID=$($AWLOCAL apigateway get-rest-apis \
#  --query "items[?name=='$API_NAME'].id" \
#  --region $REGION \
#  --output text)

if [ -z "$API_ID" ]; then
  echo "❌ Could not find LocalStack API Gateway with name: $API_NAME"
  exit 1
fi

BASE_URL="http://localhost:4566/restapis/$API_ID/$STAGE_NAME/_user_request_"

# -------------------- Step 2: Generate config.json --------------------
echo "{" > $OUTPUT_FILE

for ((i = 0; i < ${#ENDPOINTS[@]}; i++)); do
  KEY="${ENDPOINTS[$i]}Endpoint"
  VALUE="$BASE_URL/${ENDPOINTS[$i]}"

  if [ "$i" -lt "$((${#ENDPOINTS[@]} - 1))" ]; then
    echo "  \"$KEY\": \"$VALUE\"," >> $OUTPUT_FILE
  else
    echo "  \"$KEY\": \"$VALUE\"" >> $OUTPUT_FILE
  fi
done

echo "}" >> $OUTPUT_FILE

# -------------------- Done --------------------
echo "✅ Generated $OUTPUT_FILE with LocalStack endpoints:"
cat $OUTPUT_FILE

