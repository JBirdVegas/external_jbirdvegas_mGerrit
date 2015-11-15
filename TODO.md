mGerrit TODO List
=================

### Outstanding
- Reset project if current project not listed in current Gerrit
- Smarter refresh - ideal: deprecate manual refresh and refresh automatically
    - Compare change updated timestamps and change ids to determine when the rest of the data is old and does not need to be changed
	- Only load the new stuff and don't reset all the cards views if not necessary.
- Do not default to one specific gerrit - open up the list of Gerrits to choose from. This applies only when there is no app data (e.g. mgerrit has just been installed)
	- Could detect the current ROM and select that one by default
- More feedback on which Gerrit is selected
	- Change gerrit selector icon to match the current Gerrit
		- If custom Gerrit either:
			1. Use generic icon (even the application icon will do)
			2. Let user upload a custom identifying icon
- Migrate Help dialog information into long-press hints. Could show hint when long pressing on a tab or on the card stripe. This needs more thought...
- Add hook (intent filter?) that allows mgerrit to open from a Gerrit url
    - Change preferences (either temporarily or permanently) to the gerrit from the URL
    - When given a change link open that specific change
    - When given a Gerrit link just change the current Gerrit
- Ability to track projects across different Gerrits
    - Example, I could be following CM's Gerrit and view mGerrit changes on AOKP's Gerrit
    - May want to put this in a new slide-out menu
        - List all tracked projects under the the one heading (that is collapsible)
- Separate debug/release versioning
- Layout optimisations using Android Hierarchy Viewer (debug version)
- Check BroadcastReceivers across activities
	- E.g. If a broadcast response is broadcast delivered while the ProjectsList activity is open for a change query, the broadcast should be
		(re-)delivered to the activity that was listening to broadcasts matching the actual query
- Option to disable quick scroll
- Fetching changes updates projects list
- Commit message text highlighting


### Searching (Requires database), action bar
- General searching:
	- mimic Gerrit web interface queries
	- Support searching for a specific commit heading (default). e.g. searching for "commit:[translation]" will show a heading for Commits and list all the commits with "[translation]" in the title.
	- Where it is ambiguous (e.g. search results from commits and committers) show results under section headings. These section headings should be collapsible.
- Right navigation filter draw
	- Modular: users choose a supported filter type from a spinner and a layout is inflated corresponding to that filter type from where they can fill out the filter parameters.
		- Main layouts will be either a text field or a spinner
	- Filter options modify search query (present in either action bar (search) or filter draw)
	- Idea: Users can do filtering and bypass the filter draw entirely. The filter draw provides assistance on how to formulate queries that the web interface uses.
		- We are going to support new filter types (keywords) and/or new parameters for them (e.g. date range filtering for the age keyword)
- Support allintitle -
    - `intitle:<QUERY>`
	- `allintitle:<QUERY1 QUERY2 ...>` == `intitle<QUERY1> intitle:<QUERY2>`


### Change List
- Restore projects and owner cards
	- Investigate card swiping
- Titles only
    - Easiest to do with a setting in the preferences


### Tablet (master-detail layout)
- Selection indicator for the currently selected change.
    - Visual aid to show that the details pane expands the selected change (arrow perhaps?)


### Future Direction
- Authentication/Authorisation
	- Not sure if Gerrit API(s) supports this:
	    - Show system prompt such as "mgerrit is requesting access to...", such as when an Oauth2 token is requested (something similar to what happens when you use the Google Play Services APIs anyway).
	    - This will make way for mGerrit to become a full-blown Gerrit client/portal.


### More on Auth (privileged actions)
- Would be OK if we only supported G+ (Google) signin initially
- Ideal: User gets email opens in mGerrit, can login (if not already) - can we use the gmail address here? View messages for change (& diff) and post a change review (along with verification if allowed)
    - Will probably need to wait for gerrit updates (to server(s)) for this though
- Ability to post inline code comments
    - Prompts about leaving draft comments - not clear that the comments are published with the next review.
- Sign-up is probably not best handled by a mobile application - requires SSH keys to be generated to fully sign up.
- Gerrit account preference modifications
    - Track projects (sign up to emails)
    - Could even track these in mGerrit with updates view/tab


### Optional/Lower priority
- Colour each project in a separate colour
- Add tab for reviewed (verified)
    - May be useful for admins or others wishing to cherry pick changes that compile
    - Either revied in past PS or reviewed in current patch set - up to server classification
