# ForceChess

## Überblick
    
- [Beschreibung](#beschreibung)
- [Dependencies](#dependencies)
- [Tech. Spezif.](#technische-spezifikationen)

## Beschreibung

ForceChess ist eine selbstgemachte Schach-KI, 
die per reinforcement learning trainiert wird.\
Die Application bietet eine GUI mit dessen Hilfe man gegen die trainierten bots spielen, \
oder ein Match zwischen Bots beobachten kann.
Die Application bietet auch die Möglichkeit \
unterschiedliche Spielstände und Startpositionen auf verschiedenen Brettgrößen zu Spielen und Speichern.\
Des Weiteren können unterschiedlich trainierte KIs gespeichert sowie genutzt werden.

## Technische Spezifikationen

- Speichern Binär
  - KI-zustände (weights, biases, anderwärtige Informationen)
- Speichern Text
  - Boardzustände in form von [Fen-strings](https://de.wikipedia.org/wiki/Forsyth-Edwards-Notation)
- JavaFX
  - Schachbrett, sonstiges
- MVC - Modell
  - Logische state (Schachbrett)
  - View JavaFX Scene output
  - Input JavaFx Scene input
- Collections
  - modulares Speichern und lesen von KI Dateien
  - modulares Speichern von Brettzuständen
- Vererbung
  - SchachFiguren stammen von der basis Piece Klasse
- Exception
  - eigene Exceptions für besseres Debugging
  - z.B. FenException beim Einlesen von Fen-strings

## Dependencies

- JavaFx 
  - Visueller Output vom Schachbrett 
  - Menü 
  - Einstellungen
- Stockfish - Schachengine 
  - Statische Positionsanalyse 
  - Bester Zug in Position 
  - Falls Stockfish schnell genug evaluieren kann     