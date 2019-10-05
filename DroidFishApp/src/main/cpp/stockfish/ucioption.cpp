/*
 Honey, a UCI chess playing engine derived from Stockfish and Glaurung 2.1
 Copyright (C) 2004-2008 Tord Romstad (Glaurung author)
 Copyright (C) 2008-2015 Marco Costalba, Joona Kiiski, Tord Romstad (Stockfish Authors)
 Copyright (C) 2015-2016 Marco Costalba, Joona Kiiski, Gary Linscott, Tord Romstad (Stockfish Authors)
 Copyright (C) 2017-2019 Michael Byrne, Marco Costalba, Joona Kiiski, Gary Linscott, Tord Romstad (Honey Authors)

 Honey is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Honey is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <algorithm>
#include <cassert>
#include <ostream>
#include <sstream>

#include "misc.h"
#ifdef Add_Features
#include "polybook.h"
#endif
#include "search.h"
#include "thread.h"
#include "tt.h"
#include "uci.h"
#include "syzygy/tbprobe.h"

using std::string;

UCI::OptionsMap Options; // Global object

namespace UCI {

/// 'On change' actions, triggered by an option's value change
void on_clear_hash(const Option&) { Search::clear(); }
void on_hash_size(const Option& o) { TT.resize(o); }
void on_logger(const Option& o) { start_logger(o); }
void on_threads(const Option& o) { Threads.set(o); }
void on_tb_path(const Option& o) { Tablebases::init(o); }
#ifdef Add_Features
void on_book_file1(const Option& o) { polybook1.init(o); }
void on_book_file2(const Option& o) { polybook2.init(o); }
void on_book_file3(const Option& o) { polybook3.init(o); }
void on_book_file4(const Option& o) { polybook4.init(o); }

void on_best_book_move1(const Option& o) { polybook1.set_best_book_move(o); }
void on_best_book_move2(const Option& o) { polybook2.set_best_book_move(o); }
void on_best_book_move3(const Option& o) { polybook3.set_best_book_move(o); }
void on_best_book_move4(const Option& o) { polybook4.set_best_book_move(o); }

void on_book_depth1(const Option& o) { polybook1.set_book_depth(o); }
void on_book_depth2(const Option& o) { polybook2.set_book_depth(o); }
void on_book_depth3(const Option& o) { polybook3.set_book_depth(o); }
void on_book_depth4(const Option& o) { polybook4.set_book_depth(o); }
#endif

/// Our case insensitive less() function as required by UCI protocol
bool CaseInsensitiveLess::operator() (const string& s1, const string& s2) const {

  return std::lexicographical_compare(s1.begin(), s1.end(), s2.begin(), s2.end(),
         [](char c1, char c2) { return tolower(c1) < tolower(c2); });
}


/// init() initializes the UCI options to their hard-coded default values

void init(OptionsMap& o) {

    // at most 2^32 clusters.
    constexpr int MaxHashMB = Is64Bit ? 131072 : 2048;
    o["Debug Log File"]           << Option("", on_logger);
    o["Clear_Hash"]               << Option(on_clear_hash);
#ifdef Add_Features
    o["Use_Book_1"] 	          << Option(false);
    o["Book_File_1"] 	          << Option("", on_book_file1);
    o["Best_Move_1"] 	          << Option(false, on_best_book_move1);
    o["Book_Depth_1"] 	          << Option(127, 1, 127, on_book_depth1);
    o["Use_Book_2"] 	          << Option(false);
    o["Book_File_2"] 	          << Option("", on_book_file2);
    o["Best_Move_2"] 	          << Option(false, on_best_book_move2);
    o["Book_Depth_2"] 	          << Option(127, 1, 127, on_book_depth2);
    o["Use_Book_3"] 	          << Option(false);
    o["Book_File_3"]              << Option("", on_book_file3);
    o["Best_Move_3"]              << Option(true, on_best_book_move3);
    o["Book_Depth_3"]             << Option(127, 1, 127, on_book_depth3);
    o["Use_Book_4"]               << Option(false);
    o["Book_File_4"]              << Option("", on_book_file4);
    o["Best_Move_4"]              << Option(true, on_best_book_move4);
    o["Book_Depth_4"]             << Option(127, 1, 127, on_book_depth4);
#endif
#ifdef Sullivan
    o["W_Contempt"]               << Option(  22, -150, 150);
    o["B_Contempt"]               << Option(  12, -150, 150);
#else
    o["Contempt"]                 << Option(  24, -100, 100);
#endif
#ifdef Add_Features
    o["Analysis_Contempt"]        << Option("Off var White var Black var Both var Off", "Off");
#else
    o["Analysis_Contempt"]        << Option("Both var Off var White var Black var Both", "Both");
#endif
    o["Skill Level"]              << Option(40, 0, 40);
    o["Move Overhead"]            << Option(30, 0, 5000);
    o["Minimum Thinking Time"]    << Option(20, 0, 5000);
    o["Threads"]                  << Option(1, 1, 512, on_threads);
    o["Hash"]                     << Option(16, 1, MaxHashMB, on_hash_size);
    o["Ponder"]                   << Option(false);
#ifdef Add_Features
    o["7 Man Probing"]            << Option(false);
    o["BruteForce"] 	          << Option(false);
    o["Clean Search"]             << Option(false);
    o["Dynamic_Contempt"]         << Option(true);
    o["FastPlay"]                 << Option(false);
    o["Minimal_Output"]           << Option(false);
    o["No_Null_Moves"]            << Option(false);
/* NEW Adaptive Play! -  Easy for under 1400 Elo players, Medium for under 2000 Elo PLayers  and Hard for over 2000 players*/
	o["Adaptive_Play"]            << Option("Off var Novice var Advanced var Expert var Off", "Off");
    o["UCI_LimitStrength"]        << Option(false);
/* Expanded Range (1000 to 2900 Elo) and roughly in sync with CCRL 40/4, anchored to ShalleoBlue at Elo 1712*/
    o["UCI_Elo"]                  << Option(1750, 1000, 2900);
    o["UCI_Sleep"]                    << Option(false);
    // A separate weaker play level from the predefined levels below. The difference
    // between both of the methods and the "skill level" is that the engine is only weakened
    // by the reduction in nodes searched, thus reducing the move horizon visibility naturally
    o["Engine_Level"]             << Option("None var World_Champion var Super_GM "
                                            "var GM var Deep_Thought var SIM var Cray_Blitz "
                                            "var IM var Master var Expert var Class_A "
                                            "var Class_B var Class_C var Class_D var Boris "
                                            "var Novice var None", "None");
    // Score percentage evalaution output, similair to Lc0 output is now the default
    o["Score_Output"]             << Option("ScorPct-GUI var CentiPawn var ScorPct var ScorPct-GUI"
                                         ,"ScorPct-GUI");
#endif
#if (defined Add_Features && defined Sullivan)
    o["DC_Slider"]                << Option(65, -180, 180);
#elif (defined Add_Features && Stockfish)
	o["DC_Slider"]                << Option(0, -180, 180);
#endif
#ifdef Sullivan
	o["MultiPV"]                  << Option(1, 1, 256);
#else
    o["MultiPV"]                  << Option(1, 1, 500);
#endif
#if (defined Pi && defined Add_Features)
    o["Bench_KNPS"]               << Option (200, 100, 1000);//used for UCI Play By Elo
#elif (defined Add_Features)
    o["Bench_KNPS"]               << Option (1500, 500, 6000);//used for UCI Play By Elo
#endif
#ifdef Add_Features
    o["Jekyll_&_Hyde"]            << Option(0, 0, 15);
    o["Tactical"]                 << Option(0, 0,  8);
    o["Variety"]                  << Option(0, 0, 15);
#endif

#ifdef Add_Features
    o["Slow Mover"]               << Option(100, 10, 1000);
#else
    o["Slow Mover"]               << Option(84, 10, 1000);
#endif

    o["nodestime"]                << Option(0, 0, 10000);
    o["UCI_Chess960"]             << Option(false);
    o["UCI_AnalyseMode"]          << Option(false);
    o["SyzygyPath"]               << Option("<empty>", on_tb_path);
    o["SyzygyProbeDepth"]         << Option(1, 1, 100);
    o["Syzygy50MoveRule"]         << Option(true);
    o["SyzygyProbeLimit"]         << Option(7, 0, 7);
#ifndef Add_Features
	//Stockfish method of play by Elo, a very nice  implementation.
	o["UCI_LimitStrength"]        << Option(false);
	o["UCI_Elo"]                  << Option(1350, 1350, 2850);
#endif
}

/// operator<<() is used to print all the options default values in chronological
/// insertion order (the idx field) and in the format defined by the UCI protocol.

std::ostream& operator<<(std::ostream& os, const OptionsMap& om) {

  for (size_t idx = 0; idx < om.size(); ++idx)
      for (const auto& it : om)
          if (it.second.idx == idx)
          {
              const Option& o = it.second;
              os << "\noption name " << it.first << " type " << o.type;

              if (o.type == "string" || o.type == "check" || o.type == "combo")
                  os << " default " << o.defaultValue;

              if (o.type == "spin")
                  os << " default " << int(stof(o.defaultValue))
                     << " min "     << o.min
                     << " max "     << o.max;

              break;
          }

  return os;
}


/// Option class constructors and conversion operators

Option::Option(const char* v, OnChange f) : type("string"), min(0), max(0), on_change(f)
{ defaultValue = currentValue = v; }

Option::Option(bool v, OnChange f) : type("check"), min(0), max(0), on_change(f)
{ defaultValue = currentValue = (v ? "true" : "false"); }

Option::Option(OnChange f) : type("button"), min(0), max(0), on_change(f)
{}

Option::Option(double v, int minv, int maxv, OnChange f) : type("spin"), min(minv), max(maxv), on_change(f)
{ defaultValue = currentValue = std::to_string(v); }

Option::Option(const char* v, const char* cur, OnChange f) : type("combo"), min(0), max(0), on_change(f)
{ defaultValue = v; currentValue = cur; }

Option::operator double() const {
  //assert(type == "check" || type == "spin");		//macOS clang 6.0 error
  return (type == "spin" ? stof(currentValue) : currentValue == "true");
}

Option::operator std::string() const {
  //assert(type == "string");	//macOS clang 6.0 error
  return currentValue;
}

bool Option::operator==(const char* s) const {

  //assert(type == "combo");	 //macOS clang 6.0 error
  return    !CaseInsensitiveLess()(currentValue, s)
         && !CaseInsensitiveLess()(s, currentValue);

}


/// operator<<() inits options and assigns idx in the correct printing order

void Option::operator<<(const Option& o) {

  static size_t insert_order = 0;

  *this = o;
  idx = insert_order++;
}


/// operator=() updates currentValue and triggers on_change() action. It's up to
/// the GUI to check for option's limits, but we could receive the new value
/// from the user by console window, so let's check the bounds anyway.

Option& Option::operator=(const string& v) {

  assert(!type.empty());

  if (   (type != "button" && v.empty())
      || (type == "check" && v != "true" && v != "false")
      || (type == "spin" && (stof(v) < min || stof(v) > max)))
      return *this;

  if (type == "combo")
  {
      OptionsMap comboMap; // To have case insensitive compare
      string token;
      std::istringstream ss(defaultValue);
      while (ss >> token)
          comboMap[token] << Option();
      if (!comboMap.count(v) || v == "var")
          return *this;
  }

  if (type != "button")
      currentValue = v;

  if (on_change)
      on_change(*this);

  return *this;
}

} // namespace UCI
