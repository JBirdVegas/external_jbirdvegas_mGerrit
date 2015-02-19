mGerrit
=======

[![Build Status](https://api.travis-ci.org/JBirdVegas/external_jbirdvegas_mGerrit.png?branch=master)](https://api.travis-ci.org/JBirdVegas/external_jbirdvegas_mGerrit)

Gerrit instance viewer supporting the most popular Android ROMs. Supports tracking the Gerrit Instances for
-   AOKP
-   AOSP
-   CarbonROM
-   CM
-   Gerrit Review
-   Omni
-   PAC-man
-   PA
-   Add your own...

View commits by status Reviewable (Open), Merged, Abandoned;

Quickly and easily view commit message, author, owner, committer, files changed and approvals and more. 

Quickly view diffs in browser.

Simple and easy to use UI makes casual gerrit code review change viewing enjoyable.

Open source: <https://github.com/aokp/external_jbirdvegas_mGerrit>

This app is included in the Android Open Kang Project (ROMControl > About > AOKP Code Review).

Thanks AOKP translators for the translations! We have the best support community in Android!

Simplified Chinese translations by YULIANGMAX and Czech translation by Petr Reznicek (kecinzer)


## Change List Searching
Searching the change lists, either the 'Review', 'Merged' or 'Abandoned' tabs is designed to mimic the functionality in the Gerrit
 web interface. As each change contains a lot of different data, Gerrit uses what are known as operators. To avoid confusing these
 with what they call boolean operators we will call these "keywords".

**Keywords**: Act as restrictions on the search. As more keywords are added to the same query string, they further restrict the returned
 results.

As an example, searching for `subject:warnings` will return a list of changes where the commit subject contains the string "warnings"
 in the locally stored changes for the current tab.

Note: these search queries operate on the data stored locally in the database and do not query the Gerrit server. It is recommended
 to refresh the data before querying to make sure the most recent changes will be available to be searched.
Currently, it is required to re-submit the search query when changing tabs as each one operates independently.

If the search does not match the format of one of the following keywords, it will be treated as a message search. So it is sufficient to
 enter just a portion of the title of the commit you are searching for. This is new in mGerrit v2.10.010.

### Supported keywords
Currently the following keywords are supported:

`change:'ID'`; `changeid:'ID'`
The change-Id that was scraped out of the commit message. Performs a partial match, so only the first few characters need to be specified.

`message:'MESSAGE'`; `subject:'MESSAGE'`; `intitle:'MESSAGE'`
Changes that match MESSAGE arbitrary string in the first line of the commit message. The string MESSAGE can be quoted (") which will
 only show changes that contain exactly the string MESSAGE. If the string is unquoted and contains spaces, only the first word will
 be used.

`owner:'USER'`
Changes originally submitted by USER. Identifier that uniquely identifies one account. The user can be:
- a string of the format "Full Name <email@example.com>"
- just the email address ("<email@example>")
- a full name if it is unique ("Full Name")
- an account ID ("18419")

`project:'PROJECT'`
Changes occurring in PROJECT. Exact match only.

`topic:'TOPIC'`
Changes whose designated topic at upload was TOPIC. This is often combined with branch: and project: keywords to select all related changes in a series.

`branch:'BRANCH'`
Changes for BRANCH. This is the short name shown in the web interface without the 'refs/heads/' prefix.

`#'NUMBER'`; `no:'NUMBER'`
Changes with commit number 'NUMBER'. This is the legacy commit number and is shown as the title of the change details screen and also uniquely identifies a change in the Gerrit web interface

`age:'OP''AGE'`
    Amount of time that has expired since the change was last updated with a review comment or new patch set. Unlike the Gerrit web interface, this supports both specifying absolute and relative times.

**Absolute**:
 'AGE' must be of a form specified by the ISO standard, where the date is mandatory and the time is optional. `YYYY-MM-DDTHH:mm:ss` fits this format. Should be in server time as no datetime conversion is performed.

**Relative**:
'AGE' can be provided as a period of time since the current time, for example to view changes 2 days ago use: `age:"2 days"`
The age must be specified to include a unit suffix, for example `age:2d`

The supported unit suffices are:
- s, sec, secs, second, seconds
- m, min, mins, minute, minutes
- h, hr, hrs, hour, hours
- d, day, days
- w, week, weeks
- mon, mons, mth, mths, month, months
- y, yr, yrs, year, years
Multiple units can be specified in a single query, but they must be separated with a space. The order of the units is not important. A space between the unit and its suffix is optional.
The following query is valid:
    `age:">2 years 4 days 3w  1d 2m"`

The supported operators are:
- `<`: Search for changes earlier than (before) the given time
- `>`: Search for changes later than (after) the given time
- `<=`: Search for changes earlier than or equal to the given time
- `>=`: Search for changes later than or equal to the given time
- `=`: Default. Search for changes equal to the given time give or take one unit from the shortest time interval. For example the search:
	`age:"=5 weeks 2 days"`	is equivalent to `age:">5 weeks 1 days" age:"<5 weeks 3 days"`
Note: Specifying the same unit multiple times in a single keyword stacks, with the value accumulating. For example:
`age:"5 weeks 2w"`	is equivalent to `age:"7 weeks"`


License
-------
mGerrit is licensed as Apache v2
```
Copyright (C) 2013 Android Open Kang Project (AOKP)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
```


Building
--------
mGerrit building instructions (from base of repository):
- ANT: `ant clean debug`
- gradle: `gradle clean build`
 
gradle requires Build Tools revision 19.1.0


AOKP
----
mGerrit's AOKP Embedded version:
This software comes included in the AOKP firmware stack. The apk is included
as a signed prebuilt apk.  This allows updating from the PlayStore, avoids
provider authority collisions and removes mGerrit from AOKP version dependency.

To find the prebuilt apk please see `$TOP/vendor/aokp/prebuilt/common/mGerrit.apk`
