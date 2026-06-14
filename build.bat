RMDIR /S /Q ForceChess
jpackage --input build-input/ --name ForceChess --main-jar user-1.0-SNAPSHOT-fat.jar --main-class org.mxnik.forcechess.main.SuperMain --type app-image
cd ForceChess
mkdir boardsNBots
cd boardsNBots
mkdir bots
copy nul FenBoards.properties
copy nul option.properties
cd bots
mkdir networks
mkdir sample_data