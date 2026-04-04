# Hotel Management System (HMS)
### A JavaFX + Gradle desktop application

---

## 📦 Project Structure

```
hms/
├── build.gradle                          # Gradle build config
├── settings.gradle
├── gradle/wrapper/
│   └── gradle-wrapper.properties
└── src/main/
    ├── java/com/hms/
    │   ├── HMSApplication.java           # Main app + all UI tabs
    │   ├── Room.java                     # Room model (JavaFX properties)
    │   ├── Customer.java                 # Customer / booking model
    │   ├── LogEntry.java                 # Activity log model
    │   └── BillGenerator.java            # PDF invoice via iText7
    └── resources/
        └── styles.css                    # Dark luxury theme
```

---

## 🚀 Prerequisites

| Tool       | Version  |
|------------|----------|
| Java JDK   | 17 or 21 |
| Gradle     | 8.x (via wrapper) |

No manual JavaFX install needed — Gradle downloads it automatically.

---

## ▶️ Run the Application

```bash
# From the project root:
./gradlew run          # macOS / Linux
gradlew.bat run        # Windows
```

---

## 📁 Bill Storage

PDF bills are saved automatically to:

```
~/HMS_Bills/INV-<id>.pdf
```

After checkout you will be prompted with an **"Open Bill"** button to view it directly.

---

## ✨ Features

### Dashboard
- Live stat cards: Total Rooms · Occupied · Available · Revenue
- Occupancy rate progress bar (bound to live data)
- Recent activity feed

### Room Management
- Add rooms with type, number, and nightly price
- Delete unoccupied rooms
- Real-time search bar (by number, type, status)
- Toggle "Available Only" filter
- Color-coded status badge (green = available, red = occupied)

### Booking & Checkout
- Book a room by filling the form or clicking the available rooms table
- Full booking list with check-in timestamps
- **Checkout** generates a PDF invoice automatically
  - Calculates nights stayed
  - Applies 12% GST
  - Opens PDF after generation

### Activity Log
- Every check-in, checkout, and room change is timestamped and logged
- Color-coded event badges
- Searchable and clearable

---

## 🎨 Design

Dark luxury hotel theme:
- **Colors**: Deep navy (`#0F1729`) + slate surfaces + gold accents (`#D4AF37`)
- **Typography**: System font stack, weight-contrast hierarchy
- **Tables**: Alternating rows, gold column headers, status badges
- **Buttons**: Gold primary, red danger, subtle secondary
- **Forms**: Dark inputs with gold focus ring
