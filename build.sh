rm -rf ForceChess
jpackage --input build-input/ --name ForceChess --main-jar user-1.0-SNAPSHOT-fat.jar --main-class org.mxnik.forcechess.main.SuperMain --type app-image
cd ForceChess || exit
mkdir boardsNBots
cd boardsNBots || exit
mkdir bots
touch FenBoards.properties
touch option.properties
cd bots || exit
mkdir networks
mkdir sample_data