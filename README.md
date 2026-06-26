# JavaGame

## Voraussetzungen
- Java 17 oder neuer
- [H2 Database](https://h2database.com/html/main.html) JAR (z. B. `h2-2.x.x.jar`) im Projektordner `lib/`

## Build & Start
```powershell
# kompilieren (bitte H2-Jar-Pfad anpassen)
javac -cp lib\h2-2.x.x.jar -d out @(Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })

# starten
java -cp "out;lib\h2-2.x.x.jar" main
```

## Gameplay Basics
- WASD: bewegen
- Pfeile: zielen
- Leertaste: schießen (halten für Auto-Fire)
- R: Neustart nach Game Over

Beim Game Over erscheint ein Prompt zur Namenseingabe. Score + Name landen in der H2-Highscore-Tabelle, die direkt im Overlay angezeigt wird (`name | score`).
