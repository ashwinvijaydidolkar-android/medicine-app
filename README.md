# Ragini · Medicine Reminder

An Android app that **speaks** medicine reminders out loud, loud enough to hear from the next room, even when the phone is locked, on silent or on standby. It was built for elderly users: big text, big buttons, a voice they like, and no tapping needed to hear a reminder.

Works on **Android 6.0 (Marshmallow) and every newer version**, on any phone brand.

---

## What it does

### Reminders that cannot be missed
- **Starts 19 minutes before** each medicine and speaks **every 2 minutes**: at 19, 17, 15, 13, 11, 9, 7, 5, 3 and 1 minute before.
- **At medicine time it does not stop until every medicine is answered.** The chosen tune plays, then the voice reads each medicine **one by one** ("Number 1: Metformin, 500 mg, with food. Number 2: …"). Tune and voice repeat, reading only the medicines still left, until each one has been tapped **Taken** (or *Not taken* with a reason). There is no other way to silence a due medicine, and **there is no time limit**: the phone stays awake and keeps ringing until the last medicine is answered.
- **Each medicine is its own spoken message.** The opening line, every medicine, and the closing line are spoken separately with short pauses, so even a list of 30 medicines is never cut off. The music always finishes before the voice starts, so names are never covered by the tune.
- **Clear, loud words.** The message is always the plain sentence "It is time to take your medicine(s)", followed by each name, dose and food rule. Speech is kept to a clear pitch and an unhurried speed. The words are rendered to audio first and played through Android's loudness enhancer, which has a built-in limiter, so they are louder than normal text-to-speech without distorting.
- Both numbers can be changed: the start time for each medicine separately, and the gap for all medicines (Settings → Reminder rhythm).
- **Fully automatic.** Nobody has to press anything to hear a reminder.
- **Loud and clear on standby.** It uses Android's alarm-clock mechanism (`setAlarmClock`), so Doze and battery saving can't delay it. Sound plays on the **alarm channel**, so it's heard even in silent mode. The alarm volume is raised to maximum while speaking and put back afterwards. A wake lock keeps the phone awake. The screen turns on and a full-screen reminder appears over the lock screen.
- **Any battery level.** Alarm-clock scheduling works in battery saver too. When the battery is at 20% or less, the reminder also says "Please put the phone on charge". At 15% it texts the family member once a day (if the family alert is on).
- Optional extras for someone who is away from the phone or hard of hearing: a strong **vibration** pattern, a musical **chime** before the voice, and **torch blinking**.
- **Follow-up reminders** after the time if there is still no answer (2 by default, 5 minutes apart).
- Survives restarts, app updates, and time or time-zone changes (it re-registers everything).

### Attendance, exactly as specified
1. The reminder rings and shows each medicine of that time as its own numbered card.
2. On each card the user taps **Taken**, or *I can't take this one* plus a reason (pick one, or write their own; a reason is required). The music stops when the last card is answered.
3. Answers are saved straight away, but **attendance is written only at the end of the day** (11:59 PM by default; can be 10 PM, 11 PM or 11:30 PM).
   - Taken → **present** (taken)
   - Not taken + reason → **present** (not taken; the reason is kept)
   - No answer → **absent**

   If the phone was switched off at day-end, the missing days are closed the next time it wakes up.

### Reports
**Yesterday · Daily · Overnight · Weekly · Monthly · Quarterly · Six-monthly · Yearly**

- Attendance % and Taken % rings, plus Taken / Not taken / No-answer counts
- Single-day reports show the **attendance register**: each dose, its status, the reason, and when it was answered (on time or late)
- Weekly: seven day columns. Monthly to yearly: a **calendar heatmap**; tap any day for its numbers
- By medicine, by time of day (morning/afternoon/evening/night), on-time %, usual delay, current and best **streak** of perfect days
- Top reasons for not taking a dose
- **Share** the report with one tap (WhatsApp, SMS, email) to family or the doctor
- *Overnight* covers 8 PM yesterday to 8 AM today. Today's doses show as "live" until the day closes.

### 200 reminder tunes and chimes
- **100 tunes from four musical traditions**, 25 each, all composed for Ragini (not copies of existing songs):
  - **Japan:** koto, shakuhachi flute, shamisen, music box and soft piano, in Japanese in-, yo-, Okinawan and hirajoshi scales.
  - **Korea:** gayageum, daegeum flute, haegeum fiddle, yanggeum dulcimer and celesta, over the janggu drum.
  - **America:** acoustic guitar, banjo, harmonica, soft piano and vibraphone. Styles: country morning, cowboy waltz, Sunday hymn, easy blues and a little march.
  - **Britain:** Celtic harp, tin whistle, fiddle, celesta and pipe organ. Styles: English pastoral, Scottish air, country jig, old ballad and a Victorian waltz.
- **100 chimes:** tubular bells, wind chimes, glass harmonica, singing bowl, handbells, glockenspiel, crystal bells, vibraphone, hall chime and temple bell, each in ten patterns (ding-dong, rising, cascade, clock tower, sparkle, and more).
- Every sound is made on the device, so there are no audio files to download. The tune always finishes before the voice speaks, so medicine names are never covered by music.

### 108 voices, plus the family's own
- **12 languages × 9 personalities = 108 voices**: English (India, US, UK, Australia), Hindi, Marathi, Gujarati, Bengali, Tamil, Telugu, Kannada, Malayalam. The personalities are Gentle Dawn, Sunny Cheer, Monsoon Calm, Doctor's Desk, Temple Bell, Raga Melody, Little Grandchild, Wise Elder and Quick Nudge. Each has its own pitch, speed, wording and chime (wind chimes, marimba, bansuri flute, temple bell, sitar…). There are no play buttons: tapping a voice selects it and plays it straight away.
- **A voice from family.** Record a message ("Maa, it's time for your medicine. Love, Ashwin") and it plays before the medicine name is spoken.
- A different voice can be set for each medicine.
- Reminders say the user's name, the medicine, the dose, and the food rule ("Take it after food").

> **About the voices:** the speech comes from the phone's own text-to-speech engine (Google Speech Services on most phones). Each of the 108 personalities picks a different installed voice where the phone has several for that language, then shapes it with its own pitch, speed, words and chime. How many truly *different-sounding* base voices there are depends on what is installed. Google's engine has several per language, and Indian languages may need a free one-time download (the app shows **Get** next to those). A language that isn't installed falls back to Indian English with the same personality, so a reminder is never silent.

### A design that follows the day
- **Your own times of day.** Settings → *My times of day*: name the parts of your day the way you think of them ("After breakfast", "Before lunch", "Bedtime") and choose when each starts. Each lasts until the next one starts. Today's medicines, the day wheel and the reminder screen all use these names. All names are in English. The standard list is Early morning (5 AM), Morning (8 AM), Afternoon (12 PM), Evening (5 PM), Night (8 PM) and Late night (11 PM).
- **Living sky.** The colours change through the day: orange at dawn, blue at noon, rose at dusk, indigo at night. Settings → *Sky* can keep one colour instead.
- **The day wheel.** Today opens on a round dial of the day, noon at the top, with one petal for each of your times of day. Every dose is a bead on the wheel; a sun or moon shows the time now. Below it, a string of beads fills as medicines are taken.
- **A balanced bar.** Today · Medicines · the big **+** in the middle · Voices · Reports. Settings is the gear at the top.
- **The reminder screen** wears the dark sky of the medicine's own time, with the time framed in an arch and one bead and one card per medicine.

### Moving medicines to another device
- **Medicines → Move medicines to another device** (also in Settings).
  - **Send to another phone** shares one small file through WhatsApp, Bluetooth, e-mail or Drive.
  - **Save as a file** puts it in Downloads or Google Drive.
  - **Load a list from a file** opens one. On the other phone, tapping the file in WhatsApp and choosing *Ragini* also opens it.
- Before anything changes, Ragini shows the medicines it found. **Add to my medicines** keeps the current list and adds every medicine from the file – nothing is skipped, even one with the same name and times. **Replace my whole list** asks twice.
- The file carries the medicines, their times, schedules, food rules, stock and your times of day. Attendance history stays on the device where it happened.
- The same file works in the **Windows app** (Medicines → *Save my list to a file* / *Load a list from a file*), so a list can move phone → PC and PC → phone.

### "Earlier today" check-in
- Right after medicines are added to Ragini (a new install, an imported list or medicines typed in), Ragini looks for doses of those medicines that were already due earlier today, before they were added.
- It asks about each one: **Taken**, or **Not taken** with a reason. **I took all of them** answers them all at once.
- Answers count in Today, the reports and the attendance register exactly like answers given to a reminder.
- **I don't remember – skip** closes the question. Skipped doses are left out of today's attendance, as before.
- Ragini asks only once, and only about medicines added in the last 30 minutes.

### Updating the app
- Install a new Ragini APK over the one already on the phone. Medicines, times of day, settings and attendance history are kept, and reminders are set again straight after the update.
- Every GitHub build gets a higher version number and the same signing key, so Android always accepts it as an update. Do not uninstall first: uninstalling deletes the medicines.
- After the first update to this version, Ragini says once that your medicines are all still there and offers **Send my list to another phone**.
- **Medicines → Move medicines to another device → Get the latest version** opens the download page.

### Made for older eyes and hands
- Lexend typeface (designed for reading ease), extra-large text, and a text-size slider up to 150%
- Every button is at least 64 dp tall; each "Taken" button on the reminder is 76 dp
- Each medicine has its own **colour and pill shape** (capsule, tablet, oval, syrup, drops, injection), so it can be recognised without reading
- **Day dial** on the home screen: a 24-hour clock with noon at the top (sun) and midnight at the bottom (moon). Each dose sits at its time; the marigold dot is "now".
- Light and dark themes
- **Medicine names entered correctly:** the keyboard's autocorrect is switched off for names, spelling suggestions appear as you type (about 230 common medicines plus the ones you have already entered), there is an optional "Say it as" spelling for names the voice mispronounces, and the editor shows the exact sentence that will be spoken
- **No limit on medicines per time:** add as many medicines as needed for 8:00 PM in one go (Add another medicine at this time). Each is saved as its own medicine, with its own colour and its own Taken button

### Thoughtful extras
- **Pill count & refill warning**: counts down as doses are taken and warns before they run out (it also says so out loud)
- **Family alert by SMS**: if a dose is still unanswered after every reminder, a text goes to a family member (optional)
- **Call family** button on the reminder screen
- **"Will it ring on standby?" checklist** that finds every phone setting that could silence reminders (notifications, exact alarms, full-screen permission, battery optimisation, alarm volume, speech engine, and auto-start on Xiaomi, Redmi, Poco, Oppo, Realme, Vivo, iQOO, OnePlus, Huawei, Honor and Asus) and opens the right page to fix it
- **Test reminder in 1 minute**: lock the phone and check it rings
- Course length: Ongoing, or 3/5/7/10/14/30/90 days. Schedules can be every day, some weekdays, or every N days.
- Works fully **offline**. No account, no internet, no ads, nothing leaves the phone (except the optional SMS).

---

## Getting the installable APK

The Android build tools come from Google's servers, so the APK is built in the cloud by GitHub, for free:

1. Create a **private** repository on GitHub and upload this whole folder (or `git push` it).
2. Open the **Actions** tab. The **Build Ragini APK** workflow starts by itself (or press **Run workflow**).
3. After about 5 minutes, open the finished run and download **Ragini-MedicineReminder-APK** (a zip containing `Ragini-MedicineReminder.apk`).
4. Copy the APK to the phone, open it, and allow "Install unknown apps" when asked.

**Or with Android Studio:** open this folder, wait for Gradle sync, then *Build → Build App Bundle(s)/APK(s) → Build APK(s)*.

Every build is signed with the same key (`keystore/ragini-release.jks`), so new versions install **over** the old one without losing medicines or history. Keep the repository private.

**Deploying to managed devices:** the APK can be uploaded as an *Android line-of-business app* in Microsoft Intune (or any MDM) and pushed to Android Enterprise devices. Pre-grant notifications and exact alarms, and exclude the app from battery optimisation through your device policy.

## First-run checklist on the phone

The app walks through these on first launch. They're also under **Settings → Will it ring on standby?**

1. Allow **notifications**.
2. Allow **Alarms & reminders** (Android 12+).
3. Allow **full-screen notifications** (Android 14+).
4. Set battery to **Unrestricted / Don't optimise**.
5. On Xiaomi/Redmi/Poco, Oppo/Realme, Vivo/iQOO, OnePlus, Huawei/Honor: switch on **Auto-start** and, if offered, **lock the app in recent apps**.
6. Press **Test reminder in 1 minute**, lock the phone, and wait.

## Notes for publishing on Google Play
- `USE_EXACT_ALARM` is granted automatically but Google Play limits it to alarm and calendar apps. For a Play listing, remove it and rely on `SCHEDULE_EXACT_ALARM` (the app already asks the user for it).
- `SEND_SMS` needs a Play policy declaration. Remove it if the family-alert SMS isn't needed.
- Replace the family signing key with your own upload key.

## Project layout

```
app/src/main/java/com/ragini/medreminder/
├── alarm/      AlarmService (speaks, max volume, wake lock, vibration, torch)
│               ReminderActivity (full-screen over the lock screen: one numbered card per medicine, each with Taken)
│               Notifs, ActionReceiver, CaregiverAlert
├── schedule/   AlarmScheduler (setAlarmClock), Doses (the 19/17/…/1 rhythm), DayCloser (end-of-day attendance),
│               Answers, Boot/Alarm receivers
├── voice/      VoiceCatalog (108 voices), Phrases (12 languages, one-by-one), Speaker (loud TTS on the alarm channel),
│               TuneSynth (the 200 tunes), Chimes (tune playback), VoiceRecorder (family voice)
├── report/     Reports (8 periods, attendance, streaks, share text)
├── data/       Db (SQLite), Prefs, Models
└── ui/         Today (day dial), Medicines, Editor, Voices, Reports, Settings, Onboarding, theme
```

Kotlin · Jetpack Compose · Material 3 · plain SQLite · no third-party libraries beyond AndroidX.
