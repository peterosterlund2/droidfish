# Introduction

*DroidFish* is a feature-rich graphical chess user interface, combined with
the very strong *Stockfish* chess engine.

*DroidFish* is primarily designed for engine analysis of chess positions and
viewing and editing of chess games. It can also be used for playing games, either
against a chess engine or against another human player. Both players must play
on the same device though.

A much weaker chess engine called *CuckooChess* is also included in
*DroidFish*. Its primary feature is that it can be made to play very weakly so
that even beginners have a reasonable chance to beat it.

<a href="https://f-droid.org/repository/browse/?fdid=org.petero.droidfish" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>


# Using the user interface

* Many common actions are invoked by tapping on user interface elements such as
  buttons, chess pieces and text.

* A context menu can often be opened by long pressing (tap and hold) on an
  element such as the chess board, the move list text area or a button.

* The *Left drawer menu* contains command actions and is opened by swiping from
  the left side of the screen towards the middle. The *right drawer menu*
  contains less common actions and is opened by swiping from the right side of
  the screen towards the middle.  
  It is also possible to open the left/right drawer menu by tapping on the
  left/right half of the app title bar.

* To play a move on the board first tap the piece to move, then tap the
  destination square. Alternatively, touch the piece to move, drag it to the
  destination square and release the piece. For pawn promotion moves a context
  menu is opened that lets the user select the piece to promote to. For castling
  moves, first tap on the king and then tap on the king destination square. The
  rook is moved automatically.  
  **Note!** Castling is only allowed if the king and rook have not previously
  been moved. When setting up a position manually (see below), make sure to also
  set the castling right flags appropriately.


# Permissions

*DroidFish* requests the *Storage* permission when it is first started. This
permission is used to read/write data in the `DroidFish` directory on the
external storage. *DroidFish* does not read/write any file outside of the
`DroidFish` directory, except when explicitly requested to save/load a PGN
(portable game notation) or FEN/EPD (Forsyth-Edwards notation / extended
position description) file in a different directory.

The `DroidFish` directory is used to store opening books, PGN files, FEN/EPD
files, third party chess engines and tablebase files. It is also used to store
some settings, such as UCI parameter values.

It is possible to use *DroidFish* without granting the *Storage* permission, but
functionality will be rather limited.


# Default buttons

By default the following buttons are displayed next to the chess board, from
left to right:

* The folder button (Custom Button 3). This button opens the last used file to
  let you select a PGN game or a FEN/EPD position. This button only has an
  effect if you have previously opened a file.

* The light bulb button (Custom Button 2). This button toggles engine
  analysis. See the **Game mode** section below for details.

* The rotate board button (Custom Button 1). This button rotates the chess board
  180 degrees.

* The game mode button. See the **Game mode** section below for details.

* The left arrow button. This button moves to an earlier position in the
  game. See the note in the **Game mode** section below for details.  

* The right arrow button. This button moves to a later position in the game. See
  the note in the **Game mode** section below for details.  

Tap and hold a button to display a menu with additional actions.

See the **Button configuration** section below for information about how to
configure button actions.


# Game mode

Change the *game mode* by tapping the `M` button. There are three types of game
modes:

1. To view and/or edit a game, use the *Edit/re-play Game* game mode. In this
   mode there are no chess clocks and no chess engine that will make moves for
   any side.

1. To make the chess engine analyze the position on the chess board, use the
   *Analysis mode* game mode. In this mode the engine will continuously analyze
   the current position. During analysis it is possible to step back/forward in
   the game and/or add/remove moves to explore possible variations. All moves
   played on the board are automatically added to the game tree.

1. To play games, use one of the modes *Play white*, *Play black*, *Two
   players*, *Computer vs computer*. In these modes chess clocks are used and
   when a computer engine is playing it will limit its thinking time to respect
   the time available on the clock.

**Note!** When the left/right arrow buttons are used to move back/forward in a
game, the position will move one or two half-moves depending on the game mode
and current position. If a human player is playing against a computer player,
the buttons will move to the next/previous position where it is the human's turn
to make a move. To make the buttons move only one half-move, change the game
mode to *Edit/re-play game* or *analysis mode*.

**Hint!** Since enabling and disabling analysis mode can be a very common
operation while analyzing a game, there is a special button (the one with the
light bulb image) that toggles analysis mode. When analysis mode is disabled the
game mode that was used before analysis mode was enabled is restored.


# Playing strength

For engines that can reduce their playing strength using the UCI_LimitStrength
and UCI_Elo options, it is possible to specify the engine playing strength by
opening the *Left drawer menu* and selecting *Set Engine Strength*. Both
built-in engines (*Stockfish* and *CuckooChess*) can reduce their playing
strength.

The available Elo range can be different for different engines. If *Stockfish*
is playing too strong even on the lowest setting, consider switching to the
*CuckooChess* engine which is able to play at a much weaker level. At the lowest
setting, *CuckooChess* plays random legal moves so it should be usable also for
an absolute beginner.

The selected playing strength is shown in the title bar after the engine name.

Playing strength changes take effect the next time the engine starts to think
about a move.

The playing strength setting is only used in game playing mode. When the engine
is in analysis mode, full strength is always used.


# The move list text area

The move list keeps a record of moves played during a game and during analysis.
Tap on a move in the move list to move to the corresponding point in the game.

The behavior of the move list can be configured in several ways by settings
available in *Left drawer menu* -> *Settings* -> *Other* -> *PGN Settings*. You
can specify if variations, comments and annotations should be used when viewing,
importing and exporting PGN data.

By default the move list is automatically scrolled to make the current move
visible at the top of the move list text area. To disable this behavior change
the setting *Left drawer menu* -> *Settings* -> *Behavior* -> *Auto scroll move
list*.

If you do not want any variations to be used while playing or analyzing a game,
enable the setting *Left drawer menu* -> *Settings* -> *Behavior* -> *Discard
variations*.  
**Note!** In this mode any move played on the board immediately becomes the
mainline and the previously played move (if different from the just played move)
and all following moves and variations are immediately removed from the game
tree.

Tap and hold the move text area to open a menu with the following actions:

* *Edit Headers*: Opens a dialog where the standard PGN headers can be edited.

* *Edit comments*: Opens a dialog where comments and annotations for the current
  move can be edited. The following fields are available:
  * *Before*: Edit the comment before the current move.
  * *After*: Edit the comment after the current move.
  * *Move*: The move itself cannot be edited but the annotation for the move can
    be edited. Valid annotations are:
    * `!  ` : Good move
    * `?  ` : Poor move
    * `!! ` : Very good move
    * `?? ` : Very poor move
    * `!? ` : Speculative move
    * `?! ` : Questionable move
    * `=  ` : Equal chances, quiet position
    * `∞  ` : Unclear position
    * `+/=` : White has a slight advantage
    * `=/+` : Black has a slight advantage
    * `+/-` : White has a moderate advantage
    * `-/+` : Black has a moderate advantage
    * `+- ` : White has a decisive advantage
    * `-+ ` : Black has a decisive advantage

* *Add opening name*: Adds or updates the `ECO` and `Opening` PGN headers based
  on information from the ECO (Encyclopedia of Chess Openings) database and the
  main line in the current game.

* *Truncate Game Tree*: Removes the current move and all following moves and
  variations from the game tree.

* *Move Variation Up/Down*: Rearrange the order of variations in the game tree.
  Note that you may have to move a variation up several times to make it become
  the main line.

* *Add Null Move*: Adds a null move to the game tree. A null move does not move
  any piece but passes the turn to the other side. Adding a null move can be
  useful to perform threat analysis.  
  **Note!** A null move is not a legal move in chess.


# Hints about the current position

*DroidFish* can display different kinds of hints to improve the user's
understanding of the current position on the board.

## Engine analysis

When the engine is analyzing a position, information about the current position
is displayed at the bottom of the screen. Information is also displayed while
the engine is thinking about what move to play in a game, if *Left drawer menu*
-> *Settings* -> *Hints* -> *Show Computer Thinking* is enabled.

The first line of information has the following format:

[*depth*] *score* *principal_variation*

* *Depth* is the search depth the engine used when it calculated the score and
  the principal variation. Search depth is measured in number of half-moves.

* The *score* is a measure of how good the engine thinks the current position is
  from the white player's point of view. A positive number means white is better
  and a negative number means black is better.

  The score is either a numerical value, where 1.00 is roughly equal to a one
  pawn advantage, or a mate score in the form m*value*, where *value* is the
  number of moves to mate. If black is winning *value* is a negative number.

  In some cases the engine has not calculated an exact score, only an upper or
  lower bound for the score. In this case the score is prepended with either
  `<=` or `>=` to show that the score is not exact.

  **Hint!** To instead show scores from the perspective of the side to move, go
  to *Left Drawer Menu* -> *Settings* -> *Hints* and disable *White-based
  scores*.

* The *principal variation* shows the best game continuation for both sides
  according to the engine.

The last line of information has the following format:

d:*depth*[/*selDepth*] *i*:*move* t:*time* n:*nodes* nps:*speed* h:*hashfull* tb:*tbhits*

* *depth* The search depth to which the engine is searching the current move.

* *selDepth* The selective search depth, if reported by the engine.

* *i* is the move number the engine is currently searching. An engine generally
  starts searching the move it thinks is best, then continues with the second,
  third, etc best moves until all moves have been searched. Then it starts over
  with a larger search depth.

* *move* The move the engine is currently searching.

* *time* The amount of time in seconds the engine has been searching in the
  current position.

* *nodes* The number of nodes the engine has searched for the current position.

* *speed* The search speed measured in number of nodes per second.

* *hashfull* How full the engine hash (transposition) table is. This is
  displayed as a percentage value between 0 and 100.

* *tbhits* How many successful tablebase lookups the engine has performed for
  the current position. This is only displayed if the value is larger than 0.

The displayed information can be configured in different ways by long pressing
on the analysis text area when it is visible. A context menu appears with the
following choices:

### Add Analysis

Select *Add Analysis* to add the moves from the principal variation to the
current game.

### Number of Variations

Select *Number of Variations* to control how many best lines the engine will
calculate. By default only the best line of play is calculated. Change this to a
larger number to show information about the N best lines of play. This option is
not displayed if not supported by the currently used engine.

**Note!** This setting is only used during analysis, not when the engine is
playing a game. During game play only the best line of play is calculated to
avoid a speed loss that would make the engine play weaker.

### Show whole variations / Truncate variations

Select this option to enable or disable the display of the full PV the engine
has computed. The default (*Truncate variations*) is to only display the first
few moves that fit on a single line on the screen.

### Hide statistics / Show statistics

If you are not interested in the last line that displays statistics about the
engine (as opposed to the other lines that display information about the chess
position), select *Hide statistics* to hide that information.

## Opening book moves

If the current board position is included in the currently used opening book,
information about all book moves for the position is displayed at the bottom of
the screen. Each move is displayed as *move*:*percentage* where *percentage*
shows how often the computer player would play that move in games.

Opening book hints can be enabled/disabled from *Left Drawer Menu* -> *Settings*
-> *Hints* -> *Show Book Hints*. Note that the chess engine will use the opening
book even if opening book hints are disabled.

## Opening name (ECO codes)

The setting *Show ECO codes* controls when ECO codes and opening names are
displayed. There are three options:

* `Off`: ECO codes and opening names are never displayed.
* `Auto`: ECO codes and opening names are displayed in the opening phase
  up to 5 moves after the last move in the ECO database.
* `Always`: ECO codes and opening names are displayed during the whole game.

## Arrows

*DroidFish* sometimes draws arrows on the chess board to give information about
the current position.

Go to *Left Drawer Menu* -> *Settings* -> *Hints* -> *Use Arrows* to control how
many arrows to display.

Go to *Left Drawer Menu* -> *Settings* -> *Appearance* -> *Color Settings* to
change the arrow colors. Note that manually made color changes are overwritten
if you set a new color theme using *Left Drawer Menu* -> *Set Color Theme*.

The arrows have different meanings in different circumstances. The first that
applies from the following list defines what the arrows mean:

1. If *DroidFish* is in analysis mode or if it is *DroidFish's* turn to move and
   *Show Computer Thinking* is enabled, engine search information is
   displayed. If *Number of Variations* is 1, the first N half-moves from the
   principal variation is displayed. If *Number of Variations* is larger than 1,
   the best N moves are displayed. (To change *Number of Variations*, tap and
   hold on the analysis output area while the engine is thinking.)

1. If *Left Drawer Menu* -> *Settings* -> *Hints* -> *Show Book Hints* is
   enabled and the current position is included in the opening book, the arrows
   display the N best moves from the opening book.

1. If there are variations recorded in the current game for the current
   position, the arrows display the first N variations. Variations appear for
   example if you undo a move and play a different move instead.

## Endgame tablebase information

To change how endgame tablebase hints are displayed, go to *Left drawer menu* ->
*Settings* -> *Endgame Tablebases* and change *Show Hints* and/or *Edit Board
Hints*.

When *Show Hints* is enabled and the position on the board is included in an
endgame tablebase, if you tap on a piece, information about all legal moves for
that piece is displayed. For each square the piece can move to, the tablebase
score is displayed on that square. The score is positive if the side to move has
the advantage.

When *Edit Board Hints* is enabled, you are using the board editor to set up a
position, and the position is included in an endgame tablebase, if you select a
piece on the board, information about alternative positions for that piece is
displayed. For each square the piece can be placed on, the tablebase score for
the corresponding position is displayed on that square. The score is positive if
white has the advantage.

A tablebase probe can produce one of three different types of scores, depending
on what tablebase file was probed:

* Distance to mate. This is displayed as +*score* or -*score*, where `+` is used
  for winning scores and `-` is used for losing scores. The *score* value is the
  number of moves (not half-moves) to mate. A drawn position is displayed as
  `0`.
  * A Gaviota tablebase probe produces a distance to mate score.

* Distance to zeroing move. This is displayed as W*score* or L*score*, where `W`
  is used for winning scores and `L` is used for losing scores. The *score*
  value is the number of moves (not half-moves) to the next zeroing move. A
  drawn position is displayed as `0`.
  * A Syzygy `.rtbz` tablebase probe produces a distance to zeroing move score.
  * A zeroing move is a move that resets the 50-move draw counter, that is a
    pawn move or a capture.

* Win/draw/loss. This is displayed as `W`, `0` or `L`.
  * A Syzygy `.rtbw` tablebase probe produces a win/draw/loss score.


# Claim/Offer/Accept Draw

*DroidFish* does not automatically end a game when the 50-move rule or the
3-fold repetition rule is triggered. According to FIDE (International Chess
Federation) rules, these conditions do not automatically end a game, they just
allow a player to claim a draw if the player wants to.

To request a draw action, select *Right drawer menu* -> *Claim/Offer/Accept
Draw*. The following happens:

* If the current position allows a draw to be claimed, the game immediately ends
  with a draw result.

* If the other player offered a draw in the previous move, the draw offer is
  accepted and the game immediately ends with a draw result.

* Otherwise, play a move on the board as usual.

* If the resulting position allows a draw to be claimed, the game immediately
  ends with a draw result.

* Otherwise the move is played on the board and a draw offer is given to the
  opponent. The opponent is free to accept or ignore the offer.

**Note!** It is not possible to offer a draw without also playing a move.

The chess engine never offers a draw, but it can accept a draw offer made by the
user and it can claim a draw if allowed by the rules of chess.

**Note!** *DroidFish* does not implement the relatively new 75-move draw rule
and 5-fold repetition rule. Those rules are primarily meant to prevent very long
games where neither player would want to claim a draw. *DroidFish* does not mind
if the players want to play very long games so it does not have to implement
these rules.


# Saving and loading games and positions

## PGN

To load a game from a PGN file, go to *Left drawer menu* -> *File* -> *Load game
from PGN file*. Select the file to read, then select the game to load from the
list of available games.

To save a game to a PGN file, go to *Left drawer menu* -> *File* -> *Save game
to PGN file*. Select the file to save to. If the file is empty, the game is
directly saved in the file. If the file is not empty, a list of existing games
is displayed. To decide where the current game should be saved in the file,
select an existing game from the list. A menu is opened where you can decide if
the current game should be saved before, after or replace the selected game.

There is a search field above the game list. If a string is entered in the
search field, the game list is filtered to only show games matching the search
string. The matching is case insensitive and by default matches a substring in
the game list. It is possible to enable matching using [Java regular expression
syntax](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html)
by enabling *Regular Expression Search* in the menu.

**Hint!** In the game list, tap and hold a game to delete that game from the PGN
file.

**Hint!** To delete the whole file, select *Delete File* from the menu.

## FEN/EPD

To load a position from a FEN/EPD file, go to *Left drawer menu* -> *File* ->
*Load position from file*. Select the file to read, then select the position to
load from the list of available positions in the file. A preview of the position
is displayed on the small chess board. Click the `OK` button to replace the
current game with the selected position. Click the `CANCEL` button or the device
back button to return to the current game.

**Hint!** Tap and hold a position in the list to immediately replace the current
game with the selected position.

Saving a position to a FEN/EPD file has not been implemented.

## Autosave

When an action is performed that causes the current game to be discarded, the
game is automatically saved in the file `DroidFish/pgn/.autosave.pgn` before
being discarded. The autosave file has a maximum size of 20 games and the most
recently autosaved game is stored first in the file. If the number of games
becomes too large, the oldest stored game is removed from the file.


## OI File Manager

If the [*OI File Manager*](https://play.google.com/store/apps/details?id=org.openintents.filemanager)
app (or a compatible file manager) is installed *DroidFish* will use the app to
select files when loading/saving PGN/FEN/EPD files. This makes it possible to
read files in any directory on the device where the user has read
access. Removable storage like USB hard disks and SD cards do not typically
allow write access from apps using normal file system operations, so *DroidFish*
will only be able to read files from such locations.

**Hint!** If the OI File Manager is installed, it can also be used to copy, move
and delete existing files when invoked to load/save a PGN/FEN/EPD file.

## Character encoding

*DroidFish* assumes that PGN/FEN/EPD files are encoded in UTF-8 format. ASCII is
a subset of UTF-8 so that will work too. Other encodings will likely cause at
least some characters to be displayed incorrectly.

## Setting up a position

To set up a position, open the *Left drawer menu* and select *Edit Board*. A
chess board editor is opened where you can move pieces around freely and
add/remove pieces. If a piece is selected in the area next to the chess board,
click a square on the chess board to change the piece on that square.

* If the square is empty or contains a different piece type, the selected piece
  is placed on the square.

* If the square contains the selected piece, the same piece but with opposite
  color is placed on the square.

* If the square contains the selected piece but with opposite color, the square
  is cleared.

If an empty square is selected in the area next to the chess board, click a
square on the chess board to clear that square.

If nothing is selected in the area next to the chess board you can move pieces
on the chess board by first clicking on the from square, then clicking on the
destination square. This works even if the from square is empty, which has the
effect of clearing the destination square.

The board editor has its own *Left drawer menu* where you can change the side to
move, castling flags, en passant file and move counters. The menu also contains
shortcuts to set up the initial position and to clear the board.

To use the set up position, click the `OK` button or the device back
button. This replaces the current game with the set up position. Click the
`CANCEL` button to discard the set up position and return to the current game.

**Note!** It is not possible to set up illegal positions since this could
potentially cause the chess engine to crash. UCI engines usually assume that
they are only asked to analyze legal positions. If the position being edited is
illegal, a warning message is displayed. If you click the `OK` button when the
position is illegal, the illegal position is discarded and the current game is
restored.


# Opening books

## Pre-installed books

*DroidFish* includes three pre-installed opening books:

1. &lt;Internal Book>: This is a small opening book that is also the default
   book.

1. &lt;ECO Book>: This opening book contains all book lines that define the ECO
   codes.

1. &lt;No Book>: This is an empty opening book that can be used when no
   opening book is wanted.

## Installing additional opening books

To use *polyglot*, *CTG* or *ABK* book files:

1. Copy one or more opening book files to the `DroidFish/book` directory on the
   external storage.

   1. Polyglot books must have the file extension `.bin`.  
     **Note!** The Android file system may be case sensitive, in which case the
     extension must be `.bin`, not `.Bin` or `.BIN`.

   1. A *CTG* book consists of three files with file extensions `.ctg`, `.ctb`
      and `.cto`. You must copy all three files.

   1. an *ABK* book must have the file extension `.abk`.

1. Go to *Left drawer menu* -> *Select opening book*.

1. Select the opening book you want to use.

*Hint!* There are many free opening books available for download from the
internet. There is also a free tool called *PolyGlot* that can be used on a PC
to create polyglot opening books from a collection of PGN games. How to find and
use these resources is outside the scope of this manual.


# UCI engines

In addition to the pre-installed chess engines, *DroidFish* can also use third
party UCI engines:

* Copy one or more UCI engine binaries to the `DroidFish/uci` directory on the
  external storage.  
  **Note!** The binaries must be compiled for Android.

* Go to *Left drawer menu* -> *Manage Chess Engines* -> *Select Chess
  Engine* and select the engine to use.

External engines are started with the current working directory set to
`DroidFish/uci/logs` on the external storage. This is useful for engines that
expect to find data files in a path relative to the current directory.

*Hint!* There are many free chess engines compiled for Android that can be
downloaded from the internet. How to find such engines is outside the scope of
this document.

## Open exchange engines

*DroidFish* supports the *open exchange chess engine interface*, which is a way
to package UCI chess engines as Android apps. If you install such an app the
engine will automatically appear in the list of available engines in
*DroidFish*.

## Changing UCI options

To change options for the current chess engine, go to *Left drawer menu* ->
*Manage chess engines* -> *Set options*. (Alternatively, tap and hold the light
bulb button and select *Engine options*.) This opens an editor where UCI options
defined by the chess engine can be edited. What options are available and what
they do depend on the chess engine. Refer to the chess engine documentation for
details.

Click `OK` to send the modified option values and selected button options to the
engine. If the engine is not thinking the options will be applied immediately.
Otherwise the options will be applied when the engine stops thinking or starts
thinking about a new position.

Engine settings are saved, so the next time you use the same chess engine it
will use the settings you saved the last time it was used. To restore default
settings, open the UCI options editor and click `RESET` and `OK`.

## Using a remote engine server

*DroidFish* can use UCI engines that run on a remote server computer.

* Install chess network server software on the remote computer.

  * For Windows and Linux, install the Engine server software from the
    [DroidFish](http://hem.bredband.net/petero2b/droidfish/index.html) page.

  * Alternatively for Linux, `mini-inetd` from the `tcputils` package can be
    used.

* Select *Manage Chess Engines* in the *Left drawer menu*, create a new network
  engine and enter the host name (or IP address) and port number for the remote
  engine.

* Go to *Left drawer menu* -> *Manage Chess Engines* -> *Select Chess Engine*
  and select the engine to use.

**Note!** The remote server protocol simply sends UCI protocol commands over a
TCP connection using plain text. There is no security or authentication built
into the protocol. Therefore a remote server should only be used in a trusted
private network.

**Note!** It is possible to access a private network over the internet by
running VPN server software in the private network. How to set up a VPN server
is beyond the scope of this document.


# Endgame tablebases

## Syzygy

*DroidFish* can use [Syzygy endgame tablebases](https://www.chessprogramming.org/Syzygy_Bases),
which can be downloaded for free from the internet. To use Syzygy tablebases:

* Copy `.rtbw` and optionally `.rtbz` files to the `DroidFish/rtb` directory on
  the external storage.

* Change settings in *Left drawer menu* -> *Settings* -> *Endgame Tablebases* to
  control how the tablebases are used.

Tablebases containing up to 7 men are supported, although it is probably
impractical to use larger than 5-men tablebases for handheld devices because of
the very large size of 6-men and 7-men tablebases.


## Gaviota

*DroidFish* can use [Gaviota endgame tablebases](http://sites.google.com/site/gaviotachessengine/Home/endgame-tablebases-1),
which can be downloaded for free from the internet. To use Gaviota tablebases:

* Copy `.gtb.cp4` files to the `DroidFish/gtb` directory on the external
  storage.

* Change settings in *Left drawer menu* -> *Settings* -> *Endgame Tablebases* to
  control how the tablebases are used.


## Tablebases for remote engines

To configure tablebases for remote engines, go to *Left drawer menu* ->
*Settings* -> *Endgame Tablebases* and change *GTB Network Directory* and
*Syzygy Network Directory* to match the paths where the tablebases are installed
on the remote computer.


# Interfacing with other apps

## Sharing games and positions

To share the current game, tap and hold the chess board and select *Share game*
or *Share as Text* from the menu. This brings up a list of applications that can
import the game data. Use *Share game* to send the PGN data to another chess
app. Use *Share as Text* to send the PGN data as text to a non-chess app, such
as an email app.

To share the current position as a PNG image, tap and hold the chess board and
select *Share as Image*.

It is also possible to import/export PGN and FEN/EPD data from/to the
clipboard. Tap and hold the chess board and select *Clipboard* to use this
function. The *Paste from Clipboard* function automatically detects both PGN and
FEN/EPD data.

*DroidFish* will also appear in the list of target apps when you share chess
data from other chess apps installed on the device.

## Scid on the go

If the [*Scid on the go*](https://play.google.com/store/apps/details?id=org.scid.android)
app is installed *DroidFish* can directly open games from Scid database files.
To use this function, tap and hold the chess board, select *File* from the menu,
then select *Load game from Scid file*. Select the desired file, then select the
game to load from the file.

**Hint!** If you later want to load a different game from the same Scid file,
use the *folder button* to go directly to the game list for the last used file.

## ChessOcr

If the [*ChessOcr*](https://play.google.com/store/apps/details?id=com.kgroth.chessocr)
app is installed *DroidFish* can use that app to scan chess positions from
magazines or books and automatically set up the scanned position in
*DroidFish*. To use this function, tap and hold the chess board and select
*Retrieve Position* from the menu. Select the ChessOcr app and follow the
instructions in that app.

If the scanned position is valid it is set as the current board position in
*DroidFish*. If the scanned position is invalid, *DroidFish* enters edit board
mode with the invalid position. Correct the position to make it valid and click
the `OK` button to use the position.


# Settings

*DroidFish* has a large number of configurable settings, which can be changed by
opening the *Left drawer menu* and selecting *Settings*. Some important settings
not already explained are described in the following sections.

## Time control

It is possible to specify the time control as the number of moves to be played
in a given amount of time. Additionally it is possible to specify a time
increment that is added to each player's clock after making a move.

Engine players try their best to respect the specified time control settings,
but if you are using a very fast time control on a slow device, it is possible
that the engine runs out of time. When the engine has no time left on its clock
it will play as fast as it can. This can cause some engines to play very weakly,
so it is best to use a slower time control if this happens on your device.

The time control is also used for human players, but note that the game does not
automatically end if a player runs out of time. It is up to the user to decide
what to do if a player runs out of time.

Time control changes take effect when a new game is started.

## Hash table size

Use *Left drawer menu* -> *Settings* -> *Engine Settings* -> *Hash Table* to
change the hash table size used by the chess engine.

In Android there is a limit for the maximum amount of Java memory an app is
allowed to use. The limit depends on the device and possibly also on the Android
version the device is running. *DroidFish* will not allow a chess engine to use
a larger hash table than this limit. This restriction is enforced because
Android is not designed to run processes requesting a lot of memory, and can
become unstable or crash or reboot if it runs out of memory.

It is possible to disable this hash table size limit by creating a file called
`.unsafehash` in the directory `DroidFish/uci`. This feature should only be used
if you have determined that it does not cause your device to become unstable.

**Note!** The hash table size limit is only enforced for engines running on the
device. Network engines are not affected.

## Text size

The text size used to display moves and analysis information can be changed
in *Settings* -> *Appearance* -> *Text Size*.

## Piece names

Use the *Piece Names* setting in the *Appearance* section to control how pieces
are represented in the move list and analysis information. There are three
available choices:

* English letters. The following letters are used:
  * `P` : Pawn
  * `N` : Knight
  * `B` : Bishop
  * `R` : Rook
  * `Q` : Queen
  * `K` : King

* Local language letters. The letters used depend on the current user interface
  language.

* Figurine notation. Small images are used instead of piece letters.

**Note!** The PGN standard says English letters should be used for identifying
piece names, so English letters are always used/assumed during PGN
export/import.

## Piece set

You can change the appearance of the chess pieces on the chess board using the
*Piece Names* option in the *Appearance* section. It is also possible to change
piece colors in *Color Settings* in the *Appearance* section. Note however that
piece colors are overwritten if you change the color theme using *Left drawer
menu* -> *Set Color Theme*.

## Move announcement

Use *Left drawer menu* -> *Settings* -> *Appearance* -> *Speak moves* to control
how played moves are announced. The following settings are available:

* *Off*: Moves are not announced.

* *Speech*: When a move is played by either a human or the computer, the played
  move is spoken using short algebraic notation. English, German and Spanish
  speech is available.

Enable *Settings* -> *Appearance* -> *Move sound* to play a sound when the
computer player makes a move.

Use *Settings* -> *Appearance* -> *Enable Vibration* to make the device vibrate
when the computer player makes a move.

## Button configuration

Button actions can be changed from *Left drawer menu* -> *Settings* ->
*Behavior* -> *Configure Buttons*.

The three leftmost buttons can be changed by the user. Each button has a main
action that is triggered when the button is tapped. The main action also
determines the icon used to display the button. Additionally up to 6 extra
actions can be defined for a button. To invoke the additional actions, tap and
hold the button, then select the desired action from the menu that opens.

If all actions for a button are disabled the button is not displayed. Note that
in *settings* the buttons are named *Custom Button 1/2/3*, where button 3 is the
leftmost button.

The three rightmost buttons (the `M` button and the left/right arrow buttons)
have predefined actions that cannot be changed by the user.

## Opening book settings

You can change aspects of the opening book from *Left drawer menu* -> *Settings*
-> *Other* -> *Opening Book Settings*. The following settings are available:

* *Book Length*: Controls the maximum number of moves the engine will play from
  the opening book. This setting does not affect opening book hints. The default
  is *unlimited*.

* *Prefer main lines*: When enabled, moves that are marked as main line moves in
  the book are given a higher weight so they will be played more often by the
  chess engine.  
  **Note!** This option only has an effect for *CTG* opening books.

* *Tournament mode*: When enabled, only book moves that are marked for
  tournament play are played by the chess engine.  
  **Note!** This option only has an effect for *CTG* opening books.

* *Book randomization*: Controls how often different book moves are played by
  the engine. The default is 50% which means that the statistics from the
  opening book is used unmodified. 100% means that all book moves are played
  with (almost) the same probability regardless of the statistics from the
  opening book. 0% means that the book move with the best statistics is played
  (almost) 100% of the time.

* *Book Filename*: This option is set automatically when *Left drawer menu* ->
  *Select Opening Book* is used. It is however possible to set this value
  manually in which case the full path to an opening book file should be
  specified (including the file extension). This can be useful if you have a
  very big opening book stored somewhere on the device but it would be
  impractical to copy it to the `DroidFish/book` directory.

**Note!** The move percentages calculated by *DroidFish* for *CTG* books are
unlikely to agree with percentages calculated by other chess programs that can
use *CTG* books.

**Note!** The move percentages calculated by *DroidFish* for *ABK* books are not
always equal to percentages shown in the Arena Chess GUI, because the algorithm
used by Arena to compute the percentages is unknown.
.board-layout-evaluation {
    display: flex;
    grid-column: evaluation
}

.board-layout-evaluation>* {
    margin-left: calc(var(--boardContainerWidth) - var(--boardWidth) - var(--gutterLeftOfBoard));
    margin-right: calc(var(--gutterLeftOfBoard) + var(--boardWidth) - var(--boardContainerWidth))
}

@media (min-width: 960px) {
    body.with-evaluation {
        --evalAndGutter: calc(var(--gutterLeftOfBoard) + var(--evalWidth));
        --evalWidth: 2rem
    }

    .board-layout-evaluation {
        width: var(--evalWidth)
    }

    .board-layout-evaluation>* {
        margin: 0 0 0 calc(var(--boardContainerWidth) - var(--boardWidth))
    }
}

.evaluation-bar-bar {
    border-radius: .2rem;
    flex-shrink: 0;
    height: 100%;
    position: relative;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
    user-select: none;
    width: 20px
}

.evaluation-bar-bar.evaluation-bar-clickable {
    cursor: pointer
}

.evaluation-bar-bar.evaluation-bar-flipped,
.evaluation-bar-bar.evaluation-bar-flipped .evaluation-bar-loader,
.evaluation-bar-bar.evaluation-bar-flipped .evaluation-bar-scoreAbbreviated {
    transform: rotate(180deg)
}

.evaluation-bar-bar.evaluation-bar-flipped .evaluation-bar-score {
    --flipTransform: rotate(180deg)
}

.evaluation-bar-bar .evaluation-bar-fill {
    background-color: hsla(0, 0%, 100%, .05);
    border-radius: .2rem;
    height: 100%;
    overflow: hidden;
    position: relative;
    width: 100%;
    z-index: -1
}

.evaluation-bar-bar .evaluation-bar-color {
    bottom: 0;
    height: 100%;
    left: 0;
    position: absolute;
    transition: transform 1s ease-in;
    width: 100%
}

.evaluation-bar-bar .evaluation-bar-white {
    background-color: #fff;
    z-index: 2
}

.evaluation-bar-bar .evaluation-bar-black {
    background-color: #403d39;
    z-index: 1
}

.evaluation-bar-bar .evaluation-bar-draw {
    background-color: #777574;
    z-index: 0
}

.evaluation-bar-bar .evaluation-bar-loader {
    height: 100%;
    width: 100%
}

.evaluation-bar-bar .evaluation-bar-loading-message {
    color: hsla(0, 0%, 100%, .6);
    font-size: 1.5rem;
    font-weight: 600;
    left: 50%;
    position: absolute;
    -webkit-text-orientation: sideways;
    text-orientation: sideways;
    text-transform: uppercase;
    top: 50%;
    transform: translate3d(-50%, -50%, 0) rotate(180deg);
    -ms-writing-mode: tb-rl;
    writing-mode: vertical-rl;
    z-index: 2
}

.evaluation-bar-bar .evaluation-bar-score {
    display: none;
    font-size: 1.2rem;
    font-weight: 600;
    -webkit-hyphens: auto;
    -ms-hyphens: auto;
    hyphens: auto;
    padding: .5rem .2rem;
    position: absolute;
    text-align: center;
    width: 100%;
    z-index: 2
}

.evaluation-bar-bar .evaluation-bar-score.evaluation-bar-dark {
    bottom: 0;
    color: #403d39
}

.evaluation-bar-bar .evaluation-bar-score.evaluation-bar-light {
    color: #fff;
    top: 0
}

.evaluation-bar-bar .evaluation-bar-score.evaluation-bar-draw {
    color: #fff
}

.evaluation-bar-bar:hover .evaluation-bar-score {
    border-radius: .3rem;
    bottom: auto;
    display: block;
    font-weight: 700;
    -webkit-hyphens: auto;
    -ms-hyphens: auto;
    hyphens: auto;
    padding: .1rem .5rem;
    position: absolute;
    text-align: center;
    top: 50%;
    transform: translate(calc(10px - 50%), -50%) var(--flipTransform, rotate(0deg));
    transition: opacity .2s;
    transition-delay: .1s;
    width: 45px;
    z-index: 2
}

.evaluation-bar-bar:hover .evaluation-bar-score.evaluation-bar-dark {
    background-color: #fff;
    color: #403d39
}

.evaluation-bar-bar:hover .evaluation-bar-score.evaluation-bar-light {
    background-color: #403d39;
    color: #fff
}

.evaluation-bar-bar:hover .evaluation-bar-score-long {
    white-space: nowrap;
    width: auto
}

.evaluation-bar-bar .evaluation-bar-scoreAbbreviated {
    font-size: 1rem;
    font-weight: 600;
    padding: .5rem 0;
    position: absolute;
    text-align: center;
    white-space: pre;
    width: 100%;
    z-index: 2
}

.evaluation-bar-bar .evaluation-bar-scoreAbbreviated.evaluation-bar-dark {
    bottom: 0;
    color: #403d39
}

.evaluation-bar-bar .evaluation-bar-scoreAbbreviated.evaluation-bar-light {
    color: #fff;
    top: 0
}

.evaluation-bar-bar .evaluation-bar-scoreAbbreviated.evaluation-bar-draw {
    background: transparent;
    color: #fff;
    top: calc(50% - 1rem)
}.board-layout-evaluation {
    display: flex;
    grid-column: evaluation
}

.board-layout-evaluation>* {
    margin-left: calc(var(--boardContainerWidth) - var(--boardWidth) - var(--gutterLeftOfBoard));
    margin-right: calc(var(--gutterLeftOfBoard) + var(--boardWidth) - var(--boardContainerWidth))
}

@media (min-width: 960px) {
    body.with-evaluation {
        --evalAndGutter: calc(var(--gutterLeftOfBoard) + var(--evalWidth));
        --evalWidth: 2rem
    }

    .board-layout-evaluation {
        width: var(--evalWidth)
    }

    .board-layout-evaluation>* {
        margin: 0 0 0 calc(var(--boardContainerWidth) - var(--boardWidth))
    }
}

.evaluation-bar-bar {
    border-radius: .2rem;
    flex-shrink: 0;
    height: 100%;
    position: relative;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
    user-select: none;
    width: 20px
}

.evaluation-bar-bar.evaluation-bar-clickable {
    cursor: pointer
}

.evaluation-bar-bar.evaluation-bar-flipped,
.evaluation-bar-bar.evaluation-bar-flipped .evaluation-bar-loader,
.evaluation-bar-bar.evaluation-bar-flipped .evaluation-bar-scoreAbbreviated {
    transform: rotate(180deg)
}

.evaluation-bar-bar.evaluation-bar-flipped .evaluation-bar-score {
    --flipTransform: rotate(180deg)
}

.evaluation-bar-bar .evaluation-bar-fill {
    background-color: hsla(0, 0%, 100%, .05);
    border-radius: .2rem;
    height: 100%;
    overflow: hidden;
    position: relative;
    width: 100%;
    z-index: -1
}

.evaluation-bar-bar .evaluation-bar-color {
    bottom: 0;
    height: 100%;
    left: 0;
    position: absolute;
    transition: transform 1s ease-in;
    width: 100%
}

.evaluation-bar-bar .evaluation-bar-white {
    background-color: #fff;
    z-index: 2
}

.evaluation-bar-bar .evaluation-bar-black {
    background-color: #403d39;
    z-index: 1
}

.evaluation-bar-bar .evaluation-bar-draw {
    background-color: #777574;
    z-index: 0
}

.evaluation-bar-bar .evaluation-bar-loader {
    height: 100%;
    width: 100%
}

.evaluation-bar-bar .evaluation-bar-loading-message {
    color: hsla(0, 0%, 100%, .6);
    font-size: 1.5rem;
    font-weight: 600;
    left: 50%;
    position: absolute;
    -webkit-text-orientation: sideways;
    text-orientation: sideways;
    text-transform: uppercase;
    top: 50%;
    transform: translate3d(-50%, -50%, 0) rotate(180deg);
    -ms-writing-mode: tb-rl;
    writing-mode: vertical-rl;
    z-index: 2
}

.evaluation-bar-bar .evaluation-bar-score {
    display: none;
    font-size: 1.2rem;
    font-weight: 600;
    -webkit-hyphens: auto;
    -ms-hyphens: auto;
    hyphens: auto;
    padding: .5rem .2rem;
    position: absolute;
    text-align: center;
    width: 100%;
    z-index: 2
}

.evaluation-bar-bar .evaluation-bar-score.evaluation-bar-dark {
    bottom: 0;
    color: #403d39
}

.evaluation-bar-bar .evaluation-bar-score.evaluation-bar-light {
    color: #fff;
    top: 0
}

.evaluation-bar-bar .evaluation-bar-score.evaluation-bar-draw {
    color: #fff
}

.evaluation-bar-bar:hover .evaluation-bar-score {
    border-radius: .3rem;
    bottom: auto;
    display: block;
    font-weight: 700;
    -webkit-hyphens: auto;
    -ms-hyphens: auto;
    hyphens: auto;
    padding: .1rem .5rem;
    position: absolute;
    text-align: center;
    top: 50%;
    transform: translate(calc(10px - 50%), -50%) var(--flipTransform, rotate(0deg));
    transition: opacity .2s;
    transition-delay: .1s;
    width: 45px;
    z-index: 2
}

.evaluation-bar-bar:hover .evaluation-bar-score.evaluation-bar-dark {
    background-color: #fff;
    color: #403d39
}

.evaluation-bar-bar:hover .evaluation-bar-score.evaluation-bar-light {
    background-color: #403d39;
    color: #fff
}

.evaluation-bar-bar:hover .evaluation-bar-score-long {
    white-space: nowrap;
    width: auto
}

.evaluation-bar-bar .evaluation-bar-scoreAbbreviated {
    font-size: 1rem;
    font-weight: 600;
    padding: .5rem 0;
    position: absolute;
    text-align: center;
    white-space: pre;
    width: 100%;
    z-index: 2
}

.evaluation-bar-bar .evaluation-bar-scoreAbbreviated.evaluation-bar-dark {
    bottom: 0;
    color: #403d39
}

.evaluation-bar-bar .evaluation-bar-scoreAbbreviated.evaluation-bar-light {
    color: #fff;
    top: 0
}

.evaluation-bar-bar .evaluation-bar-scoreAbbreviated.evaluation-bar-draw {
    background: transparent;
    color: #fff;
    top: calc(50% - 1rem)
}
.depthBarLayout {
    display: grid;
    grid-template-columns: [evaluation] var(--evalAndGutter) [pieces] var(--piecesWidth) [board] var(--boardContainerWidth) [board-controls] var(--boardControlsWidth);
    margin: -5px 0 5px auto;
    position: relative;
}

.depthBarLayout .depthBar {
    grid-column: board;
    height: 5px !important;
    width: var(--boardWidth) !important;
    display: block;
    margin-left: calc(var(--boardContainerWidth) - var(--boardWidth));
    position: relative;
    background: #403d39;
}

.depthBar .depthBarProgress {
    display: block;
    height: 100%;
    width: 0%;
    background-color: rgb(43, 194, 83);
    position: relative;
    overflow: hidden;
    font-size: 15px;
    text-align: center;
    color: white;
    transition: all 100ms ease;
}

.disable-transition {
    -webkit-transition: none !important;
    -moz-transition: none !important;
    -o-transition: color 0 ease-in !important;
    -ms-transition: none !important;
    transition: none !important;
}
/* fallback */
@font-face {
    font-family: 'Material Symbols Outlined';
    font-style: normal;
    font-weight: 400;
    src: url(https://fonts.gstatic.com/s/materialsymbolsoutlined/v71/kJF1BvYX7BgnkSrUwT8OhrdQw4oELdPIeeII9v6oDMzByHX9rA6RzaxHMPdY43zj-jCxv3fzvRNU22ZXGJpEpjC_1v-p_4MrImHCIJIZrDCvHOej.woff2) format('woff2');
}

/* fallback */
@font-face {
    font-family: 'Material Icons';
    font-style: normal;
    font-weight: 400;
    src: url(https://fonts.gstatic.com/s/materialicons/v139/flUhRq6tzZclQEJ-Vdg-IuiaDsNc.woff2) format('woff2');
}


.material-symbols-outlined {
    font-family: 'Material Symbols Outlined';
    font-weight: normal;
    font-style: normal;
    font-size: 24px;
    line-height: 1;
    letter-spacing: normal;
    text-transform: none;
    display: inline-block;
    white-space: nowrap;
    word-wrap: normal;
    direction: ltr;
    -webkit-font-feature-settings: 'liga';
    -webkit-font-smoothing: antialiased;
}


.material-symbols-outlined {
    font-size: 22px !important;
    margin-right: 5px !important;
    vertical-align: bottom !important;
    font-variation-settings:
        'FILL' 0,
        'wght' 700,
        'GRAD' 0,
        'opsz' 48
}

.material-icons {
    font-family: 'Material Icons';
    font-weight: normal;
    font-style: normal;
    font-size: 24px;
    line-height: 1;
    letter-spacing: normal;
    text-transform: none;
    display: inline-block;
    white-space: nowrap;
    word-wrap: normal;
    direction: ltr;
    -webkit-font-feature-settings: 'liga';
    -webkit-font-smoothing: antialiased;
}

.material-icons {
    font-size: 22px !important;
    margin-right: 5px !important;
    vertical-align: bottom !important;
}/* fallback */
@font-face {
    font-family: 'Material Symbols Outlined';
    font-style: normal;
    font-weight: 400;
    src: url(https://fonts.gstatic.com/s/materialsymbolsoutlined/v71/kJF1BvYX7BgnkSrUwT8OhrdQw4oELdPIeeII9v6oDMzByHX9rA6RzaxHMPdY43zj-jCxv3fzvRNU22ZXGJpEpjC_1v-p_4MrImHCIJIZrDCvHOej.woff2) format('woff2');
}

/* fallback */
@font-face {
    font-family: 'Material Icons';
    font-style: normal;
    font-weight: 400;
    src: url(https://fonts.gstatic.com/s/materialicons/v139/flUhRq6tzZclQEJ-Vdg-IuiaDsNc.woff2) format('woff2');
}


.material-symbols-outlined {
    font-family: 'Material Symbols Outlined';
    font-weight: normal;
    font-style: normal;
    font-size: 24px;
    line-height: 1;
    letter-spacing: normal;
    text-transform: none;
    display: inline-block;
    white-space: nowrap;
    word-wrap: normal;
    direction: ltr;
    -webkit-font-feature-settings: 'liga';
    -webkit-font-smoothing: antialiased;
}


.material-symbols-outlined {
    font-size: 22px !important;
    margin-right: 5px !important;
    vertical-align: bottom !important;
    font-variation-settings:
        'FILL' 0,
        'wght' 700,
        'GRAD' 0,
        'opsz' 48
}

.material-icons {
    font-family: 'Material Icons';
    font-weight: normal;
    font-style: normal;
    font-size: 24px;
    line-height: 1;
    letter-spacing: normal;
    text-transform: none;
    display: inline-block;
    white-space: nowrap;
    word-wrap: normal;
    direction: ltr;
    -webkit-font-feature-settings: 'liga';
    -webkit-font-smoothing: antialiased;
}

.material-icons {
    font-size: 22px !important;
    margin-right: 5px !important;
    vertical-align: bottom !important;
}
