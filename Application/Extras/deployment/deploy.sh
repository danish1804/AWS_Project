#!/bin/bash

echo "✅ Making scripts executable..."
chmod +x deployRegister.sh
chmod +x deploy_login_api.sh
chmod +x deploy_getSubscriptions.sh
chmod +x deploy-subscriptions.sh
chmod +x deploy-search.sh
chmod +x deploy-unsubscribe.sh


echo "🚀 Deploying Register Lambda and API..."
./deployRegister.sh
echo "✅ Register deployed."

echo "🚀 Deploying Login Lambda and API..."
./deploy_login_api.sh
echo "✅ Login deployed."

echo "🚀 Deploying Get Subscriptions Lambda and API..."
./deploy_getSubscriptions.sh
echo "✅ Get Subscriptions deployed."

echo "🚀 Deploying Subscribe Lambda and API..."
./deploy-subscriptions.sh
echo "✅ Subscribe deployed."

echo "🚀 Deploying Unsubscribe Lambda and API..."
./deploy-unsubscribe.sh
echo "✅ Unsubscribe deployed."

echo "🚀 Deploying Search Music Lambda and API..."
./deploy-search.sh
echo "✅ Search deployed."

echo "📦 All Lambda deployments complete."

