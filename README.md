# ⚒️ PrepForge — Product & System Design Document

---

# 1. 🧠 Product Vision

## Overview

PrepForge is an **AI-powered interview preparation platform** designed to simulate real-world technical interviews through structured practice, feedback, and performance tracking.

## Core Philosophy

PrepForge is built around the concept of a **forge**:

> “Through repetition, pressure, and focused effort, strength is forged.”

Users:

* Enter the forge (start sessions)
* Train under pressure (timed interviews)
* Refine their skills (AI feedback + recommendations)
* Track growth over time (dashboard + analytics)

---

# 2. 🎯 Core Features

## 2.1 Coding Interview Module

### Structure: 3 Stages

#### Stage 1 — Understanding the Metal

* User receives AI-generated problem:

  * Title
  * Description
  * Constraints
  * Example test cases
* User must:

  * Restate the problem
  * Explain approach

**Rules:**

* Time limit: ~5 minutes
* Auto-submit on timeout
* AI evaluates:

  * understanding
  * approach validity
* DOES NOT block progression

---

#### Stage 2 — Shaping the Blade

* User enters coding environment (Monaco-style editor)
* Features:

  * Starter code (imports + class)
  * Language selection
  * Run example test cases
  * Autosave
  * Timer (20–30 minutes)

**Outcomes:**

* Early submission OR
* Auto-submit on timeout

---

#### Stage 3 — Final Tempering

AI evaluates:

* Correctness
* Code quality (clean vs spaghetti)
* Time complexity
* Space complexity
* Closeness to solution

Also:

* Compares Stage 1 vs Stage 2 (planning vs execution)

Outputs:

* strengths
* weaknesses
* recommendations
* next problem suggestions

---

## 2.2 Behavioral Interview Module

### Structure

* 4–5 questions per session
* 90–120 seconds per question

### Question Types

* Failure
* Teamwork / conflict
* Leadership
* Technical challenges
* Learning new tools quickly

---

### Input Methods

* Voice (speech-to-text)
* Typed response

---

### Per Question Evaluation

AI scores:

* STAR structure
* Clarity
* Specificity
* Relevance
* Time management

---

### Session Output

* Per-question feedback
* Final summary:

  * average score
  * strongest/weakest areas
  * coaching insights
  * recommended practice areas

---

# 3. 📊 Performance Tracking System

## 3.1 Dashboard

Purpose:

> “How am I doing?”

### Sections:

* Total sessions
* Avg coding score
* Avg behavioral score
* Current streak
* Score trend chart
* Weak areas
* Recent sessions
* Active recommendations
* Motivation card

---

## 3.2 Sessions Page (History)

Purpose:

> “What have I done?”

### Each Session Includes:

* Type (Coding / Behavioral)
* Score
* Status (Submitted / Auto-submitted)
* Date
* Duration
* Summary

### Filters:

* Type
* Date range
* Score
* Completion status

---

## 3.3 Recommendations Page (Refinements)

Purpose:

> “What should I improve?”

### Sections:

* Active recommendations (top)
* Past recommendations (grouped by session)

### Each Recommendation:

* Title
* Reason
* Priority
* Source session
* CTA (Start Practice)

---

# 4. 🧭 Navigation Structure

## Sidebar Navigation

* Dashboard
* Practice

  * Coding Interview
  * Behavioral Interview
* Sessions
* Recommendations
* Profile

---

## Page Purpose Mapping

| Page            | Purpose           |
| --------------- | ----------------- |
| Dashboard       | Progress overview |
| Practice        | Start sessions    |
| Sessions        | History           |
| Recommendations | Improvement plan  |

---

# 5. 🎨 UI / UX Theme — “The Forge”

## Core Identity

Dark, focused, intense — representing discipline and growth.

---

## Color Palette

* Background: #0B0B0F
* Secondary: #12121A
* Accent: #FF6A00 (forge fire)
* Glow: #FFD166
* Text: #EAEAEA

---

## Login Page

### Layout:

* Title: **PREP FORGE**

* Quote:

  > “Consistency, Focus, and Drive forge greatness.”
  > Enter the Forge.

* CTA Button:

  > **ENTER THE FORGE**

---

## Themed Terminology

| Standard        | PrepForge       |
| --------------- | --------------- |
| Start           | Enter           |
| Submit          | Forge Result    |
| Practice        | Train           |
| Sessions        | Forged Sessions |
| Recommendations | Refinements     |

---

## Coding Stage Names

| Stage | Name                    |
| ----- | ----------------------- |
| 1     | Understanding the Metal |
| 2     | Shaping the Blade       |
| 3     | Final Tempering         |

---

# 6. 🔐 Authentication System

## Tech Stack

* Spring Boot + Spring Security
* JWT Access Tokens
* Refresh Tokens (HttpOnly cookies)
* PostgreSQL

---

## Auth Flow

### Register

* email + password
* password hashed (BCrypt)
* email verification required

---

### Login

* returns access token
* sets refresh token cookie

---

### Token Strategy

| Token         | Purpose             |
| ------------- | ------------------- |
| Access Token  | API authentication  |
| Refresh Token | Session persistence |

---

### Endpoints

* POST /api/auth/register
* POST /api/auth/login
* POST /api/auth/refresh
* POST /api/auth/logout
* GET /api/users/me

---

## Security Features

* Password hashing (BCrypt)
* HttpOnly cookies
* Role-based access
* Generic login errors
* Rate limiting (future)
* Email verification
* Password reset email flow

---

# 7. ⚙️ Backend Architecture

## Core Services

### PrepForge API

* Auth
* Sessions
* User data
* REST endpoints

---

### AI Worker

* Question generation
* Coding evaluation
* Behavioral evaluation
* Recommendations

---

### Analytics Worker

* Score aggregation
* Trends
* Dashboard data

---

### (Future) Code Execution Worker

* Run test cases
* Validate correctness

---

## Event System (Kafka)

### Example Topics:

* coding.question.generate.requested
* coding.submission.received
* coding.feedback.generated
* behavioral.answer.submitted
* behavioral.feedback.generated
* recommendation.generated

---

# 8. ⚡ DevOps Stack

* Docker (containers)
* Docker Compose (local orchestration)
* Kafka (event streaming)
* Redis (caching + rate limiting)
* PostgreSQL (primary DB)
* GitHub Actions (CI/CD)
* Railway / AWS (deployment)

---

# 9. 🔁 User Flow

### Primary Flow

Dashboard → Practice → Session → Feedback → Dashboard

---

### Improvement Flow

Dashboard → Recommendation → Practice → Improve

---

### Reflection Flow

Sessions → Review → Practice Again

---

# 10. 🚀 Future Enhancements

* System Design Interview Module
* Real-time code execution engine
* AI voice tone analysis
* OAuth (Google login)
* Multi-factor authentication
* Resume integration
* Peer mock interviews

---

# 11. 🧠 Final Product Positioning

PrepForge is not just a practice tool.

It is:

> “A structured training environment where engineers improve through repetition, pressure, and intelligent feedback.”

---
