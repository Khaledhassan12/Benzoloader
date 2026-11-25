# UI parity plan (core acceptance criterion from Khaled, 25 Nov 16:09)

His instruction: the Benzoloader UI/UX must be extremely close to the screenshots he supplied. Scope: app UI only. The captures are a design reference. They are not published, and nothing in the app may copy their text strings, icons or other assets: layout, structure, order, spacing and colour behaviour are matched, words and icons are written independently. No advertising anywhere in the app (unchanged rule). Nothing below has been rendered or run: every statement about the current app is read from layout and theme files, and visual equivalence is UNVERIFIED until screens can be rendered and compared side by side (final stage, after the build gate lifts).

## What the 38 captures show (all viewed on contact sheets)
1. Settings hub: a plain list of ten categories, each with a rounded coloured icon tile, a title and a one-line subtitle of its contents: Downloading, Torrent, Notifications, Automation, Cataloging, Scheduler, Top window, Interface, Browser, Data.
2. One full screen per category with a back arrow and the category name: Downloading (folder, empty-file creation, auto-resume, proxy, profiles per connection type, simultaneous downloads, threads, speed, minimum thread size, error limit, pause after error, multi-threading mode, user-agent), Torrent (download, sequential, default trackers, seeding, simultaneous seeding, upload speed, share ratio, seeding time, individual settings, auto-add, watch directory, connection limits, DHT, LSD, uTP, uPnP, NAT-PMP, incoming and anonymous modes, encryption mode, port range), Notifications (pop-up, icons, priority, lock screen, buttons, statistics, vibration lengths, sounds), Automation (Wi-Fi rules, battery rules, auto-add, auto-open pages, clear finished, tile actions, after-finishing action), Cataloging (switch, categories list, per-category page with name, extensions, folder).
3. Row style: title on one line, a small green value or hint line under it, a switch at the end; dependent rows greyed out; thin dividers between groups; no cards, no shadows, no section headers inside a screen.
4. A side panel opened from the main screen: a live speed graph with a progress bar, then collapsible sections (Downloading with graph and eye icons, Torrent, Notifications, Options, Interface, Tools, App) holding the most-used switches and sliders, and a bottom bar with three actions (Exit, Settings, Browser or Home).
5. Browser: top bar with menu button, a URL field with a refresh button, a bookmarks button with a count badge and a history button with a count badge; a tabs panel (New tab, list of tabs with close buttons, an Options section with Load images, Save open tabs, Desktop mode, Pop-up tabs, Block Ads, Address bar at bottom, Swap side menus, Visit history, Enable JavaScript); an overflow menu (Back, Forward, To bookmarks, Send, Open, Close, Bookmarks, History); bookmark and history lists with icon, title, address and a row menu.
6. Palette, sampled from the JPEGs (approximate, JPEG compression and screen colour settings apply): page background about #FDFDF5 (cream), top bar and status area about #F2F2EA, primary text about #292822, secondary text about #7D7C77, accent green about #659B37 (switch thumb), switch track about #C1D6AB, hint text a darker green. All captures are the light theme. Dark theme appearance is not shown anywhere.
7. NOT in the captures: the main download list screen (it only appears blurred behind panels), the download editor, the file-properties screen, the torrent editor. A capture of the main list is the single most useful missing reference.

## Current app (read from res/ files, unrendered)
- Palette is blue (primary #1E88E5, background #F8F9FA, white surfaces). The status bar uses the primary colour. Gap: every colour token.
- Settings is one long scrolling screen (activity_settings.xml, about 380 lines) with Material sliders and switches. Gap: no hub, no per-category screens, no row style, no subtitles.
- Main screen: left navigation drawer for filters and categories, Material toolbar, FAB, a list. The reference panel is a different pattern (collapsible quick-settings panel with a graph and a three-button bottom bar). Gap: panel, graph, bottom bar.
- Browser: toolbar with back, forward, URL field and buttons, WebView, tabs menu added in code earlier. Gap: bookmarks and history screens and badges, tabs panel layout, overflow menu contents and order, Block Ads (undecided, separate from the no-ads rule), address bar at bottom, swap side menus.
- Interface switches seen in the captures (Material You, dark theme, single list, bottom action bar, top bar at bottom, colored icons, swap side menus) are not implemented; only a stored dark-mode flag exists and nothing reads it.

## Plan, in order (each step written unbuilt, then compared once screens can be rendered)
1. Theme tokens: cream and green light palette as the single source (colors.xml, themes.xml), a matching dark variant chosen by me and labelled as not from the references, status and top bar colours, text colours, switch colours, divider style. Remove the blue.
2. Preference row component and the settings hub with the ten categories; split the long settings screen into the category screens in the capture order, reusing the existing settings and wiring. Rows for settings that do not exist yet are not shown, not faked.
3. Cataloging screens (switch, categories list, per-category page) from the existing catalog code.
4. Quick-settings side panel with the collapsible sections and the bottom bar; the speed graph from SpeedTracker data.
5. Browser: top bar with badge buttons, bookmarks and history screens (needs a bookmark store; history exists as a switch only), tabs panel layout, overflow menu in the captured order.
6. Interface switches that change layout (bottom action bar, top bar at bottom, swap side menus, single list, colored icons), after steps 1 to 5.
7. Main list screen: only after Khaled supplies a capture; until then it follows the palette and row style but its layout is a guess and will be reported as such.
8. Arabic and right-to-left review of all of the above; the captured category names and several rows are Arabic.

## Risks and decisions
- Effort is large: steps 2 to 6 touch most layouts and activities. No rendering is possible before the build gate lifts, so spacing and sizes will be estimated from the images and corrected at the final comparison.
- Icons: the reference icons may not be copied; I will use my own vector icons in a matching outline style.
- "Rate this App", "Send feedback" and "Privacy policy" appear in the reference App section; the app has no Play listing yet, so those rows would link nowhere. Decision for Khaled when the section is built.
- Block Ads switch and "Single list" semantics are undecided (see SPEC_COVERAGE).
