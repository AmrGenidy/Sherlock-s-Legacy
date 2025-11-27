# Detective Game - JavaFX GUI Quick Start Guide

## Prerequisites

Before running the JavaFX GUI, ensure you have:

1. **Java Development Kit (JDK) 17 or higher**
   ```bash
   java -version
   # Should show version 17 or higher
   ```

2. **Apache Maven 3.6 or higher**
   ```bash
   mvn -version
   # Should show Maven version 3.6+
   ```

3. **JavaFX 22.0.2** (automatically downloaded by Maven)

## Installation

### Step 1: Clone or Download the Repository
```bash
cd /path/to/DetectiveGame
```

### Step 2: Install Dependencies
```bash
mvn clean install
```

This will:
- Download JavaFX 22.0.2 dependencies
- Compile all Java source files
- Prepare the application for running

## Running the Application

### Option 1: Quick Launch (Recommended)
```bash
mvn javafx:run
```

This directly launches the JavaFX GUI client.

### Option 2: Via Main Launcher
```bash
mvn clean package
java -jar target/DetectiveGametest-1.0-SNAPSHOT.jar
```

Then select:
- **Option 3**: "Join/Host Multiplayer Game (Start GUI Client)"

### Option 3: With Custom Server
```bash
mvn javafx:run -Dexec.args="192.168.1.100 5555"
```

Arguments:
- First: Server hostname/IP (default: localhost)
- Second: Server port (default: 5555)

## First-Time Setup

### Starting the Server (Required for Multiplayer)

In a separate terminal:

```bash
# Compile the project first
mvn clean compile

# Start the server
mvn exec:java -Dexec.mainClass="server.ServerMain"
```

Or use the launcher:
```bash
java -jar target/DetectiveGametest-1.0-SNAPSHOT.jar
# Select option 4: "Start Game Server"
```

The server should output:
```
========================================
  Starting Game Server...
========================================
Server listening on port 5555
```

### Launching the GUI Client

In another terminal:
```bash
mvn javafx:run
```

You should see:
```
========================================
  Detective Game JavaFX Client Started
========================================
GUI initialized. You can use either:
  - The terminal at the bottom of the window
  - Or the GUI buttons and windows
========================================
```

## Using the GUI

### Main Interface

The GUI has 5 main areas:

1. **Top Bar**: Buttons for Tasks, Journal, and Chat
2. **Center**: Room visualization with clickable elements
3. **Right Panel**: Room information (exits, suspects, objects)
4. **Bottom Left**: Terminal for commands
5. **Bottom Right**: Status display

### Basic Workflow

1. **Wait for Connection**
   - The client auto-connects to the server
   - Watch the terminal for "Successfully connected" message

2. **Navigate Menus**
   - Use the terminal to respond to prompts
   - Type numbers or commands as shown

3. **Host or Join a Game**
   ```
   # In terminal, when prompted:
   1  # To host a game
   # or
   2  # To join a game
   ```

4. **Select a Case**
   - Choose a case number from the list
   - Choose a language if multiple are available

5. **Start Playing**
   - Use terminal commands OR
   - Click on suspects/objects in the room view
   - Use Tasks, Journal, and Chat windows

### Common Commands (Terminal)

```bash
# Movement
move north
move south
go east

# Interaction
question LordAshworth
examine shattered_glass
deduce MademoiselleDupont

# Investigation
journal                    # View all entries
journal add <note>         # Add a note
look                       # Describe current room

# Multiplayer
/chat Hello!               # Send chat message
/c Quick message           # Short form

# System
help                       # Show available commands
exit                       # Leave game (gracefully)
quit                       # Exit client
```

### GUI Features

#### Tasks Window
1. Click **Tasks** button (top bar)
2. See your case objectives
3. Check off tasks as you complete them

#### Journal Window
1. Click **Journal** button (top bar)
2. **Search**: Type keyword, press Enter
3. **Add Note**: Type in text area, click "Add Note"

#### Chat Window (Multiplayer)
1. Click **Chat** button (top bar)
2. Type message, press Enter
3. Red badge shows unread count

#### Room Interaction
1. **Click suspects** (blue circles)
   - Choose "Question" or "Deduce"
2. **Click objects** (brown circles)
   - Choose "Examine" or "Deduce"
3. **Watch for speech bubbles** with responses

## Troubleshooting

### Problem: "Connection refused"
**Solution**: Make sure the server is running first
```bash
# In terminal 1:
mvn exec:java -Dexec.mainClass="server.ServerMain"

# In terminal 2:
mvn javafx:run
```

### Problem: "JavaFX runtime components are missing"
**Solution**: Maven should handle this, but if not:
```bash
mvn clean install -U
mvn javafx:run
```

### Problem: GUI doesn't show room images
**Solution**: This is normal! Placeholder images are generated automatically. You can add custom images later to:
```
src/main/resources/images/rooms/
src/main/resources/images/suspects/
src/main/resources/images/objects/
```

### Problem: Terminal commands not working
**Solution**: 
1. Make sure you've connected to the server
2. Wait for prompts in the terminal
3. Type commands exactly as shown
4. Check the terminal for error messages

### Problem: "Unable to access jarfile"
**Solution**: Build the project first:
```bash
mvn clean package
```

## Testing Multiplayer

### Two Players on Same Machine

**Terminal 1**: Start Server
```bash
mvn exec:java -Dexec.mainClass="server.ServerMain"
```

**Terminal 2**: Start Player 1 (GUI)
```bash
mvn javafx:run
```

**Terminal 3**: Start Player 2 (GUI)
```bash
mvn javafx:run
```

Or mix GUI and terminal clients:
```bash
# Terminal client instead
mvn exec:java -Dexec.mainClass="client.ClientMain"
```

### Two Players on Different Machines

**Machine 1** (Server):
```bash
mvn exec:java -Dexec.mainClass="server.ServerMain"
# Note the IP address
```

**Machine 2** (Client):
```bash
mvn javafx:run -Dexec.args="<server-ip> 5555"
# Replace <server-ip> with actual IP
```

## Single Player Mode

You don't need a server for single player:

```bash
java -jar target/DetectiveGametest-1.0-SNAPSHOT.jar
# Select option 1: "Start Single Player Game"
```

Note: Single player uses terminal only (no GUI yet).

## Next Steps

1. **Read the full documentation**: `JAVAFX_GUI_README.md`
2. **Explore case files**: Check `cases/*.json` for available cases
3. **Customize the GUI**: Edit `src/main/resources/css/detective.css`
4. **Add custom images**: Place PNGs in `src/main/resources/images/`

## Quick Reference Card

| Action | Command | GUI Alternative |
|--------|---------|-----------------|
| View journal | `journal` | Click Journal button |
| Add note | `journal add <text>` | Journal → Type → Add Note |
| Chat | `/chat <message>` | Chat → Type → Enter |
| Question suspect | `question <name>` | Click suspect → Question |
| Examine object | `examine <name>` | Click object → Examine |
| View tasks | `help` | Click Tasks button |
| Move rooms | `move <direction>` | Terminal only |

## Getting Help

1. **In-game**: Type `help` in the terminal
2. **Documentation**: Read `JAVAFX_GUI_README.md`
3. **Code**: Check `src/main/java/ui/` for GUI components

## Common Mistakes

❌ Starting client before server (multiplayer)
✅ Start server first, then client

❌ Using wrong terminal (GUI vs game terminal)
✅ Use the terminal inside the GUI window

❌ Not waiting for prompts
✅ Wait for the game to show options before typing

❌ Clicking inactive elements
✅ Only click blue (suspects) and brown (objects) circles

## Performance Tips

1. **Memory**: JavaFX needs adequate RAM (2GB+ recommended)
2. **Graphics**: Ensure graphics drivers are up to date
3. **Network**: Use wired connection for better multiplayer experience

## Enjoy the Game!

Have fun solving mysteries! The GUI enhances the experience while keeping all the original terminal functionality intact.

Happy detecting! 🔍
