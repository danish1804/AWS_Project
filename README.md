![Platform](https://img.shields.io/badge/platform-AWS-232F3E?logo=amazon-aws&logoColor=white)
![EC2](https://img.shields.io/badge/hosted%20on-EC2-blue?logo=amazon-aws)
![Language](https://img.shields.io/badge/language-Java-blue?logo=java)
![Frontend](https://img.shields.io/badge/frontend-HTML%2FJS-green?logo=html5)
![Backend](https://img.shields.io/badge/backend-AWS%20Lambda-orange?logo=aws-lambda)
![Database](https://img.shields.io/badge/database-DynamoDB-4D4D4D?logo=amazon-dynamodb)
![Storage](https://img.shields.io/badge/storage-S3-yellow?logo=amazon-s3)
![License](https://img.shields.io/badge/license-MIT-brightgreen)
![Status](https://img.shields.io/badge/status-Completed-success)

# 🎵 Music Subscription Web Application

The application allows users to register, log in, search for songs, subscribe/unsubscribe to songs, and view artist images – all hosted on AWS infrastructure.

---

## 🌐 Live Demo

🟢 Hosted on: [http://-ec2-public-dns](http://-ec2-public-dns)

---


> ⚠️ The app must run on EC2 via standard HTTP(S) ports (80/443) and be accessible through a web browser.

---

## 📦 Features

### 👤 User Authentication
- **Login Page**: Validates credentials via AWS Lambda and DynamoDB.
- **Register Page**: Adds users to DynamoDB with duplicate email detection.

### 🎶 Music Library & Subscription
- **Main Page** with:
  - 🎧 User Section: Displays logged-in username.
  - 💽 Subscription Section: Lists songs the user subscribed to with "Remove" functionality.
  - 🔍 Query Section: Enables filtered music search using title, artist, album, and year.

### ☁️ AWS Integrations
- **DynamoDB**: Stores user credentials, music metadata, and subscription info.
- **S3**: Stores and serves artist images securely.
- **API Gateway + Lambda**: Processes registration, login, subscription, removal, and querying using RESTful endpoints.
- **EC2 Ubuntu Server**: Hosts the entire web app using Apache2.

---

## ⚙️ Technologies Used

- **Frontend**: HTML, CSS, JavaScript
- **Backend**: Java (AWS Lambda Functions)
- **AWS Services**:
  - EC2 (Ubuntu 20.04 LTS)
  - DynamoDB
  - S3 (for image hosting)
  - API Gateway (HTTP methods)
  - Lambda (business logic)

---

## 📁 Project Structure

```bash
Music-project/
├── frontend/
│   ├── login.html
│   ├── register.html
│   ├── main.html
│   └── style.css
├── lambda/
│   ├── LoginUserLambda.java
│   ├── RegisterUserLambda.java
│   ├── SubscribeSongLambda.java
│   └── ...
├── utils/
│   └── UploadImagesToS3.java
├── scripts/
│   └── deploy-app.sh
├── 2025a1.json
└── README.md
```

# 📦 Extras Folder Overview

## 🔧 deployment/
Shell scripts used to deploy individual Lambda functions and API Gateway endpoints. Includes:
- `deploy-login_api.sh`: Deploys login endpoint
- `deploy-search.sh`: Deploys music search API
- `deploy-app.sh`: Full app deployment

## 🗂 config/
- `2025a1.json`: Source data for songs and artist images
- `config.json`: Dynamic API URL mapping for frontend
- `s3_image_links.json`: Output mapping of uploaded S3 image URLs

## 🖼 assets/
- `no-image.jpg`: Fallback image for missing artists
- `screenshots/`: UI previews and architecture diagrams

## 🐳 docker/
Dockerfiles and compose setup for local Lambda and API Gateway simulation

---

## 🚀 Deployment Instructions

### 1. EC2 Setup
- Launch a **free-tier Ubuntu EC2 instance**.
- Install Apache2 and Java.
- Open ports **80 (HTTP)** and **443 (HTTPS)** in security group.

### 2. S3 Bucket
- Create an S3 bucket.
- Upload artist images using `UploadImagesToS3.java`.
- Store secure image URLs.

### 3. DynamoDB Tables
- Create `login`, `music`, and `subscription` tables.
- Load users and songs from JSON via Java programs.

### 4. Lambda Functions
- Deploy all Java-based Lambda handlers.
- Set up REST API endpoints in API Gateway and integrate with Lambda.

### 5. Web Hosting
- Host HTML and JS files on EC2’s Apache2 `/var/www/html` directory.

### 6. Configuration
- Use `config.json` for dynamic API URL loading in frontend scripts.

---

## 🧠 Learning Outcomes

This project demonstrates:

- Designing secure, scalable AWS applications.
- Handling real-world CRUD operations via Lambda and API Gateway.
- Optimizing database schema and query performance in DynamoDB.
- End-to-end deployment with proper cloud architecture.

---

## 📝 License

All code is written by the authors unless explicitly cited.

---

## 🙋‍♂️ Contributors

- **Mohammed Danish Alam** (Leader)  
  Role: Backend, S3, Lambda Functions, Deployment, Documentation


---

## 📚 References

- [AWS Documentation](https://docs.aws.amazon.com/)
- [DynamoDB Best Practices](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)
- [Apache2 Setup on EC2](https://ubuntu.com/tutorials/install-and-configure-apache#1-overview)

---
