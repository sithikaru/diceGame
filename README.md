# Dice Game (Android)

A turn-based dice game Android app featuring a customizable target score, multiple rolls per turn, and an AI opponent powered by a simple heuristic strategy. Built with Kotlin and Jetpack Compose.

---

## Table of Contents

- [Features](#features)  
- [Screenshots](#screenshots)  
- [Getting Started](#getting-started)  
  - [Prerequisites](#prerequisites)  
  - [Installation](#installation)  
- [How to Play](#how-to-play)  
- [Architecture & Tech Stack](#architecture--tech-stack)  
- [AI Opponent](#ai-opponent)  
- [License](#license)  

---

## Features

- 🎯 **Customizable Target Score**: Set your own winning score before starting a game.  
- 🎲 **Up to 3 Rolls per Turn**: Roll up to three times each turn.  
- 🔒 **Selectable Dice to Keep**: After each roll, choose which dice to hold.  
- ⚖️ **Tie-Breaker Rounds**: Automatic tie-breaker if both players reach the target on the same turn.  
- 🤖 **Heuristic-Based AI**: Computer opponent uses a simple strategy to decide which dice to keep.  
- ✨ **Modern UI**: Built entirely with Jetpack Compose for smooth animations and responsive layouts.  
- 🚀 **Navigation Compose**: In-app navigation between Home, Game, and Settings screens.  

---

## Screenshots

*(Add your app screenshots here — e.g., home screen, game screen, settings screen.)*

---

## Getting Started

### Prerequisites

- Android Studio (Arctic Fox or later)  
- Android SDK 21+  
- Kotlin 1.6+  

### Installation

1. **Clone the repository**  
   ```bash
   git clone https://github.com/sithikaru/diceGame.git
   cd diceGame
   ```

2. **Open in Android Studio**  
   - Choose **Open an existing project** and select the `diceGame` folder.  
   - Let Gradle sync and resolve dependencies.  

3. **Build & Run**  
   - Select an emulator or USB-connected device.  
   - Click **Run ▶️** or use the **Shift+F10** shortcut.

---

## How to Play

1. Launch the app, set your desired **Target Score**.  
2. Tap **Start Game**.  
3. On your turn, press **Roll** (up to 3 times).  
4. After each roll, tap dice to **keep** or **release** them.  
5. Tap **End Turn** to lock in your score for that turn.  
6. Reach or exceed the target score to win — watch out for tie-breaker logic!  

---

## Architecture & Tech Stack

- **Language:** Kotlin  
- **UI:** Jetpack Compose  
- **Navigation:** Navigation Compose  
- **State Management:** ViewModel + Compose state  
- **Build:** Gradle (Kotlin DSL)  
- **Minimum SDK:** 21 (Android 5.0 Lollipop)  

---

## AI Opponent

The AI uses a simple heuristic:
- Keeps any dice showing 4–6 on the first roll.  
- If the current turn total + potential max from remaining rolls ≥ target score, holds all.  
- Otherwise, re-rolls non-kept dice until 3 rolls are used.  

Feel free to inspect or tweak `AiPlayer.kt` to experiment with different strategies!

---

## License

This project is released under the [MIT License](LICENSE).  
```
