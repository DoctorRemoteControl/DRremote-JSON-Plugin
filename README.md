# DRremote JSON Plugin

Fast, lightweight JSON editor for Eclipse with outline, syntax highlighting, formatting and validation.

## Features

### Editor
- JSON-aware editor for `.json` files
- Syntax highlighting (fully configurable via Preferences)
- Inline validation with Eclipse error markers
- Format (pretty-print) and Minify actions
- Comment support (`//` + `/* ... */`) with toggle option

### Outline View
- Structured tree of the JSON document (objects, arrays, values)
- Custom icons: root, object, array, value
- Double-click → jump to JSON in editor
- Hotkeys:
  - **Enter** → replace value  
  - **Ctrl+C** → copy value (decoded)  
  - **Ctrl+Shift+C** → copy JSON subtree
- Context menu:
  - Copy value  
  - Copy JSON  
  - Copy path (`$.foo.bar[0]`)
  - Copy tree (visible structure)
  - Copy schema (structure only)
- Toolbar:
  - Expand all  
  - Collapse all  
  - Remove comments  
  - Toggle comment handling  
  - Help (shortcuts)

### Preferences
`Window → Preferences → DRremote JSON Editor`
- Colors for keys, strings, numbers, booleans, null, braces, brackets, colon, comma, default text.

### Validation
- Runs automatically on save
- Uses Jackson
- Errors shown via IMarker (line, offset, message)

## Installation

### Quick Install (recommended)
1. Download the latest plugin JAR from GitHub  
   **https://github.com/DoctorRemoteControl/DRremote-JSON-Plugin**
2. Drop it into your Eclipse `dropins/` folder  
   → restart Eclipse.

### Development
- Clone repo  
  `git clone https://github.com/DoctorRemoteControl/DRremote-JSON-Plugin.git`
- Import as PDE Plug-in Project
- Launch via **Run → Eclipse Application**

Tested with: Eclipse 4.37, Java 21.

## Dependencies
- Jackson (bundled): core, databind, annotations
- Eclipse text editor framework

## Roadmap
- Live validation while typing
- Custom indentation settings
- More outline actions / quick fixes

## Contribute
Issues & PRs welcome:  
https://github.com/DoctorRemoteControl/DRremote-JSON-Plugin
