# accident-mapper-mobile-app
An app for streaming accidents in our local communities

#🚧 Accident Mapper Mobile Application
#📌 Project Overview

Accident Mapper is an Android-based mobile application designed to facilitate real-time road accident reporting by the public. The application allows users to report accidents using text descriptions, images, and videos, which are then displayed on a centralized dashboard categorized by time. The system aims to improve road safety awareness, incident documentation, and emergency response readiness, particularly in developing regions such as Uganda.

#🎯 Objectives
# General Objective

To design and implement a mobile application that enables real-time reporting, storage, and visualization of road traffic accidents.

# Specific Objectives

To provide secure user registration and authentication.

To enable users to report accidents with multimedia support.

To categorize accidents into recent and earlier incidents.

To display accident reports in a structured, scrollable card layout.

To store and retrieve accident data efficiently for future analysis.

🧩 Key Features

🔐 User Authentication

Signup, login, and password recovery using Firebase Authentication.

# 📝 Accident Reporting

Text-based accident descriptions.

Image capture and selection from gallery.

Video recording support.

# 🕒 Accident Categorization

Recent accidents (within the last 1 hour).

Earlier accidents (older than 1 hour).

# 🧾 Accident Feed

Scrollable card-based layout.

# Displays reporter profile, description, and media.

👤 User Profile

Popup profile view with user details.

Secure logout functionality.

# 📂 Offline-Friendly Media Storage

Base64 image encoding stored in the database (no paid cloud storage required).

#🏗️ System Architecture

The application follows a client–database architecture:

Frontend

Android application developed using Kotlin and XML.

# Backend

SQLite (depending on configuration).

# Authentication Module

SQLite Authentication.

# Data Management Module

Handles user profiles, accident reports, timestamps, and media.

# 🛠️ Technologies Used
Category	Technology
Programming Language	# Kotlin
# UI Design	XML
Database	 SQLite
Authentication	sqlite Authentication
Image Handling	url saving mode in the database
IDE	Android Studio
# Architecture	MVC-based Android Architecture
#📱 Application Modules

Login & Signup Module

Home Dashboard

Navigation Drawer

Accident Reporting Module

Media Capture & Selection

Recent & Earlier Accidents Feed

User Profile Popup

Logout & Session Management

#🚀 Installation & Setup
Prerequisites

Android Studio (latest version)

Android device or emulator (API 24+)


Steps

Clone the repository:

git clone https://github.com/your-username/accident-mapper.git


Open the project in Android Studio.

Connect the app to Firebase (Authentication & Realtime Database).

Sync Gradle files.

Run the app on an emulator or physical device.

# 📊 Future Enhancements

🌍 GPS-based accident location mapping.

🚨 Integration with emergency services.

🔔 Push notifications for nearby accidents.

📈 Analytics dashboard for authorities.

🌐 Web-based admin panel.

📚 Academic Relevance

This project demonstrates:

Practical application of mobile computing concepts.

Integration of authentication and databases.

Real-world problem-solving using software engineering principles.

Scalable system design suitable for public safety applications.

# 👨‍💻 Developer

Name: Muhanguzi Amos
Program: Bachelor of science in Computer Science
Project Type: Academic semester / Final Year Project
Institution: UTAMU University

# 📜 License

This project is developed for academic purposes.
Commercial use requires permission from the author
