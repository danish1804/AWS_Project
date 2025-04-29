# 🎵 Music Subscription Web Application

This is a cloud-based music subscription application developed for **COSC2626/2640 Cloud Computing Assignment 1** at RMIT University.

The application allows users to register, log in, search for songs, subscribe/unsubscribe to songs, and view artist images – all hosted on AWS infrastructure.

---

## 🌐 Live Demo

🟢 Hosted on: [http://your-ec2-public-dns](http://your-ec2-public-dns)

---

## 🚀 Features

- ✅ User registration & login with DynamoDB validation
- ✅ Upload & display artist images from AWS S3
- ✅ Subscribe/unsubscribe to songs
- ✅ Search by title, artist, album, year
- ✅ Fully deployed on EC2 with Apache2
- ✅ All backend logic handled via API Gateway + AWS Lambda

---

## 🛠️ Tech Stack

- **Frontend**: HTML, CSS, JavaScript
- **Backend**: AWS Lambda (Java)
- **Database**: DynamoDB
- **Object Storage**: S3
- **Hosting**: EC2 Ubuntu Server (Apache2)
- **API Layer**: API Gateway (HTTP API)

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
