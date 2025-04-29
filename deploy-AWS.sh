aws lambda create-function \
  --function-name MusicAppHandler \
  --runtime java17 \
  --role arn:aws:iam::259275311707:role/LabRole \
  --handler com.amazonaws.project.handler.AppLambdaHandler::handleRequest \
  --zip-file fileb://target/Music-project-1.0-SNAPSHOT.jar \
  --timeout 30 \
  --memory-size 512 \
  --region us-east-1
