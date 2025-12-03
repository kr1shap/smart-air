# SMART AIR ☁️

SmartAir is a kid-friendly Android application designed to help children ages **6–16** understand and manage their asthma, while giving parents the tools to track medicine use, symptoms, PEF zones, and safety alerts. Parents can selectively share their child’s data with healthcare providers through a **consise, exportable PDF/CSV report**.

This is a project for **CSCB07, FALL 2025**.

[Watch our demo here!!](https://www.youtube.com/watch?v=NicjOoLmAgQ)

---

## Screenshots 

![img.png](img.png)
![img_1.png](img_1.png)
![img_2.png](img_2.png)

## Features 💨

Features are personalized based on the user role; provider, parent and child!

### Child
- Sign in through username and password
- Monitor patterns through daily check-ins 
- Log **rescue** and **controller** medications 
- Capture pre/post “Better/Same/Worse” feelings from dose
- Animated **Inhaler Technique Helper** (seal, slow breath, hold 10s, spacer tips)
- Motivation through **badges and streaks** for good technique and adherence
- Access one-tap triage guidance to manage breathing troubles
- Enter **peak-flow (PEF)** values with automatic zone calculation

---

### Parent 
- Create, link and manage multiple children
- View a dashboard of information including:
  - Today’s asthma zone
  - Last rescue time
  - Weekly rescue count
  - 7-day & 30-day trend snippets
- Track medication inventory (purchase date, expiry, remaining amount)
- Configure planned controller schedule for adherence tracking
- Set up Personal Best (PB) for PEF calculations
- Plan out zone-specific action plans 
- Get notified with real-time alerts for children:
  - Red-zone day
  - Rapid rescue repeats (≥3 uses in 3 hours)
  - “Worse after dose”
  - Inventory expired
  - low canister (≤20%) from max 300
  - Triage escalation
- Monitor each child's patterns through daily checkins and history
- Manage toggle permissions for provider information accessiblity 

---

### Provider (Read-Only)
- View children's data based on parent given permission
Parents can share data with a healthcare provider through:
- A **one-time 7-day invite code/link**
- A **PDF or CSV export**

Providers can only view the following categories with parent's permission:
- Rescue logs
- Controller adherence summary
- Symptoms
- Triggers
- Peak-flow values
- Triage incidents
- Summary graphs and charts

### Provider Report 🔖
- Provider report contains a bar chart for zone values, and a line chart for PEF values 
- Symptom Burden Day: A day with high activity values based on daily-checkin 
- Rescue Frequency: The number of days where rescue inhaler was used / the total number of days in the period
- Notable Triage Incident: Where 3+ red flags were chosen and alarming/high rescue usage values (i.e. >= 5)
  
---

## Team Members & Contributions 

### Krisha Patel (Scrum Master)
- Sign-in & Firebase Authentication
- Account recovery
- Role selection routing & Onboarding
- Security features
- Worse dose alert
- Notification centre setup
- Streaks and Badges
- Technique Helper

### Anjali Patidar (Team Member)
- Manage children features
- Parent and child linking 
- Granular sharing
- In-app labels on items
- Behaviour toggles
- Provider accessibility
- Invitation flow

### Faiza Khan (Team Member)
- Dashboard tiles for parent
- Shareable PDF for provider report
- Adherence

### Jennifer Huang (Team Member)
- One-tap triage session and alerts for child
- Set start at home action plan to each zone for parent
- Inventory page functionality and alerts
- Rapid rescue repeats

### Tharjiha Suthekara (Team Member)
- Daily check-in & triggers
- History browser
- Generate CSV/PDF for History
- Child dropdown list
- PEF, Personal Best and zone calculations
- Incident log
- Red-zone day alerts
- Toolbar

### Zupaash Naveed (Team Member)
- Medicine logs (rescue & controller)
- Medication home page
- Rescue badge calculations
- Pre/Post Check
- Inventory UI
