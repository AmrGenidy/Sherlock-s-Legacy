# Detective Game - JavaFX GUI Implementation

## Overview

This document describes the complete JavaFX GUI implementation for the Detective Game. The GUI provides a modern, interactive interface while maintaining full compatibility with the existing terminal-based client and server architecture.

## Features Implemented

### 1. **Main GUI Layout** (BorderPane)
- **Top Panel**: Navigation buttons for Tasks, Journal, and Chat windows with unread message indicator
- **Center Panel**: Room visualization with clickable suspects and objects
- **Right Panel**: Information display for neighboring rooms, suspects, and objects
- **Bottom Panel**: Split view with terminal console and status area

### 2. **Terminal Integration**
- Full terminal console at the bottom of the screen
- Text input field for manual command entry
- System.out redirection to GUI terminal
- Both GUI buttons and terminal commands work seamlessly

### 3. **Journal Window**
- Search bar with real-time filtering
- Scrollable list of journal entries
- Text area for adding new notes
- "Add Note" button executes `journal add <note>` command
- Maintains synchronization with game state

### 4. **Chat Window** (Multiplayer)
- Scrollable chat history display
- Message input field with Enter-to-send
- Automatic `/chat` prefix wrapping
- Unread message counter on main Chat button
- Counter resets when window is opened

### 5. **Tasks Window**
- Displays case tasks and recommendations
- Checkboxes for marking tasks as completed
- Visual feedback (strikethrough, color change)
- Loads tasks from case JSON data

### 6. **Room Visualization**
- Dynamic room loading from RoomDescriptionDTO
- Background image display (with placeholder generation)
- Clickable suspects and objects positioned on screen
- Hover effects on interactive elements
- Room name display at top

### 7. **Interactive Elements**
- **Suspects**: Click to show dialog with "Question" and "Deduce" options
- **Objects**: Click to show dialog with "Examine" and "Deduce" options
- Dialog selections execute corresponding commands
- Speech bubble responses appear above clicked elements

### 8. **Speech Bubbles**
- Appear above suspects/objects after interaction
- Display NPC responses or examination results
- Auto-fade after 3 seconds
- Smooth transitions

### 9. **Styling & Theme**
- Dark detective/mystery theme (Victorian-inspired)
- Gold (#d4af37) accent color throughout
- Custom CSS for all components
- Hover effects and transitions
- Consistent color scheme

## Architecture

### Package Structure
```
ui/
├── GameClientFX.java          # JavaFX Application launcher
├── MainController.java        # Main GUI controller
├── util/
│   ├── TextAreaOutputStream.java    # Console redirection
│   ├── RoomView.java                # Room visualization component
│   ├── GameOutputParser.java       # Terminal output parser
│   ├── PlaceholderImageGenerator.java  # Generates placeholder images
│   └── ImageResourceLoader.java     # Image loading with fallback
└── windows/
    ├── JournalWindow.java     # Journal window
    ├── ChatWindow.java        # Chat window
    └── TasksWindow.java       # Tasks window
```

### Resources
```
resources/
├── fxml/
│   └── main.fxml              # Main layout definition
├── css/
│   └── detective.css          # Complete styling
└── images/
    ├── rooms/                 # Room background images
    ├── suspects/              # Suspect icons
    └── objects/               # Object icons
```

## Integration with Existing Code

### GameClient Modifications
The `GameClient` class has been modified to support GUI input:

1. **Input Queue**: Added `BlockingQueue<String> guiInputQueue`
2. **enqueueUserInput()**: New public method for GUI to send commands
3. **Modified run() loop**: Checks both GUI queue and console scanner
4. **Non-blocking**: Works seamlessly with both terminal and GUI input

### Key Integration Points
- Commands from GUI buttons are sent via `mainController.sendCommand()`
- Commands are enqueued to GameClient via `gameClient.enqueueUserInput()`
- GameClient processes them using existing command parser and factory
- All game logic remains unchanged

## Running the Application

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- JavaFX 22.0.2 (automatically handled by Maven)

### Launch Methods

#### Method 1: Via Maven
```bash
mvn clean compile
mvn javafx:run
```

#### Method 2: Via Main Launcher
```bash
mvn clean package
java -jar target/DetectiveGametest-1.0-SNAPSHOT.jar
# Then select option 3: "Join/Host Multiplayer Game (Start GUI Client)"
```

#### Method 3: Direct Launch
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="ui.GameClientFX"
```

### Command-Line Arguments
```bash
mvn javafx:run -Dexec.args="localhost 5555"
```
- First argument: Server host (default: localhost)
- Second argument: Server port (default: 5555)

## Usage Guide

### Starting a Game

1. **Launch the GUI Client**
2. **Connect to Server**: The client automatically attempts to connect
3. **Main Menu**: Use the terminal or wait for server response
4. **Host or Join Game**: Follow the prompts in the terminal

### Using the GUI

#### Terminal Commands
- Type commands in the bottom text field
- Press Enter to send
- All standard game commands work (move, question, examine, etc.)

#### Tasks Window
- Click "Tasks" button to open
- Check off tasks as you complete them
- Provides visual progress tracking

#### Journal Window
- Click "Journal" button to open
- **Search**: Type keyword and press Enter to filter entries
- **Add Note**: Type in text area and click "Add Note" button
- Notes appear immediately in the list

#### Chat Window (Multiplayer)
- Click "Chat" button to open
- Type message and press Enter or click "Send"
- Unread messages show a counter badge on Chat button
- Counter resets when window is opened

#### Room Interaction
- Click on **suspects** (blue circles) to Question or Deduce
- Click on **objects** (brown circles) to Examine or Deduce
- Speech bubbles show responses
- Hover over elements for visual feedback

### Keyboard Shortcuts
- **Enter** in terminal input: Send command
- **Enter** in chat input: Send message
- **Enter** in journal search: Filter entries

## Placeholder Assets

The GUI includes automatic placeholder generation when image files are not found:

### Room Backgrounds
- Gradient from dark brown to dark blue
- Textured for atmospheric effect
- 800x600 default size

### Suspects
- Blue circular icons with silhouette
- Labeled with suspect name
- 80x80 size

### Objects
- Brown diamond-shaped icons
- Labeled with object name
- 80x80 size

### Adding Real Assets
To replace placeholders with actual images:

1. Place PNG files in appropriate directories:
   - Rooms: `/src/main/resources/images/rooms/`
   - Suspects: `/src/main/resources/images/suspects/`
   - Objects: `/src/main/resources/images/objects/`

2. File naming convention:
   - Lowercase
   - Spaces replaced with underscores
   - Example: "Ballroom" → `ballroom.png`
   - Example: "Lord Ashworth" → `lord_ashworth.png`

## Customization

### Changing Colors
Edit `/src/main/resources/css/detective.css`:
- Gold accent: `#d4af37`
- Dark background: `#1a1a1a`
- Terminal green: `#00ff00`

### Modifying Layout
Edit `/src/main/resources/fxml/main.fxml`:
- Adjust component sizes
- Change positioning
- Add new UI elements

### Adding Features
1. Create new window class in `ui/windows/`
2. Add button in `main.fxml`
3. Add handler in `MainController.java`

## Testing

### Manual Testing Checklist
- [ ] GUI launches without errors
- [ ] Terminal output appears in TextArea
- [ ] Terminal input sends commands
- [ ] Tasks window opens and closes
- [ ] Journal window search works
- [ ] Journal note adding works
- [ ] Chat window sends messages
- [ ] Chat unread counter increments
- [ ] Room view displays correctly
- [ ] Suspects are clickable
- [ ] Objects are clickable
- [ ] Dialog buttons execute commands
- [ ] Speech bubbles appear and fade
- [ ] CSS styling is applied
- [ ] Hover effects work

### Testing with Server
1. Start game server: `java -cp target/DetectiveGametest-1.0-SNAPSHOT.jar server.ServerMain`
2. Start GUI client: `mvn javafx:run`
3. Test multiplayer features

## Known Limitations

1. **No Live Testing**: Implementation completed in non-Java environment
2. **Image Assets**: Placeholder images are programmatically generated
3. **Terminal Dependency**: Still requires existing GameClient terminal logic
4. **Single Window**: Multiple clients require multiple application instances

## Future Enhancements

### Potential Additions
- Sound effects for actions
- Background music
- Animated transitions between rooms
- Mini-map display
- Inventory panel
- Case notes panel
- Achievement system
- Save/Load game state UI

### Advanced Features
- Drag-and-drop for notes
- Right-click context menus
- Keyboard shortcuts for all actions
- Customizable themes
- Screen reader support
- Multi-language UI

## Troubleshooting

### GUI Doesn't Launch
- Ensure Java 17+ is installed: `java -version`
- Check JavaFX dependencies: `mvn dependency:tree | grep javafx`
- Try clean rebuild: `mvn clean compile`

### Terminal Not Showing Output
- Check TextAreaOutputStream initialization
- Verify System.out redirection
- Restart application

### Images Not Loading
- Check file paths in resources directory
- Verify naming convention (lowercase, underscores)
- Placeholder generation should work automatically

### Commands Not Working
- Verify GameClient.enqueueUserInput() is called
- Check terminal for error messages
- Ensure server is running and connected

## Architecture Decisions

### Why BlockingQueue?
- Thread-safe communication between GUI and GameClient
- Non-blocking for JavaFX Application Thread
- Maintains command order

### Why Separate Windows?
- Better organization and focus
- User can keep windows open across rooms
- Independent lifecycle management

### Why Terminal Integration?
- Maintains backward compatibility
- Allows power users to use commands
- Useful for debugging
- Gradual learning curve

### Why FXML?
- Separation of concerns (view vs. controller)
- Easy to modify layout without code changes
- Scene Builder compatible
- Standard JavaFX practice

## Credits

Implementation based on the original terminal-based Detective Game architecture with full preservation of existing game logic, command system, and network communication.

## License

Follows the same license as the main Detective Game project.
