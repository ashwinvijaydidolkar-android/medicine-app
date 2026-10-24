/*
 * RAGINI REPORT CARD – one design for every way a report is seen or sent.
 *
 * The same file is used by the Windows app and (as an asset) by the Android app, so the report on
 * screen, the PDF, the spreadsheet (CSV) and the text message always say the same thing in the same order:
 *
 *   1. Masthead      – what report, for whom, which dates
 *   2. Verdict       – one big attendance ring, two tiles either side, and one plain sentence
 *   3. Days          – a calendar of coloured day squares (or, for a single day, the day's timeline)
 *   4. Time of day | Reasons   – two balanced columns
 *   5. Medicines     – one row per medicine with a bar
 *   6. Register      – every dose, the attendance register itself
 *
 * Input (R):
 * { period: 'weekly', title: 'Weekly report', from: '2026-10-18', to: '2026-10-24', person: 'Aai',
 *   madeAt: <ms>, closeLabel: '11:59 PM', streak: { current, best },
 *   slots: [{ name, start }], colors: ['#5B54D6', …],
 *   rows: [{ day: 'YYYY-MM-DD', minute, name, color, status: 'taken'|'not'|'missed'|'pending', reason, answeredAt: <ms>|null, scheduledAt: <ms>, final }] }
 *
 * Plain JavaScript, no libraries, no network – it works offline inside the apps.
 */
'use strict';

const RaginiReport = (() => {
  const STATUS = {
    taken: { label: 'Taken', light: '#1F8A70', dark: '#2AA07E' },
    not: { label: 'Not taken', light: '#BF7B0E', dark: '#BD821C' },
    missed: { label: 'No answer', light: '#B8406E', dark: '#D65A8C' },
    pending: { label: 'Still to answer', light: '#8C87A0', dark: '#8C87A0' }
  };
  const MED_COLORS = ['#5B54D6', '#F2A93B', '#1F9E89', '#E0607E', '#3A9BDC', '#9B5DE5', '#6BAF3C', '#D9734A'];
  const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
  const WD = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const pad = n => String(n).padStart(2, '0');
  const parse = d => { const [y, m, dd] = d.split('-').map(Number); return new Date(y, m - 1, dd, 12); };
  const iso = dt => `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}`;
  const addDays = (d, n) => { const t = parse(d); t.setDate(t.getDate() + n); return iso(t); };
  const dow = d => (parse(d).getDay() + 6) % 7;            // Monday = 0
  const minuteLabel = m => { const h = Math.floor(m / 60) % 24, mm = m % 60; return `${h % 12 || 12}:${pad(mm)} ${h < 12 ? 'AM' : 'PM'}`; };
  const longDate = d => { const t = parse(d); return `${t.getDate()} ${MONTHS[t.getMonth()]} ${t.getFullYear()}`; };
  const shortDate = d => { const t = parse(d); return `${t.getDate()} ${MONTHS[t.getMonth()].slice(0, 3)}`; };
  const stamp = ms => { const t = new Date(ms); return `${t.getDate()} ${MONTHS[t.getMonth()].slice(0, 3)} ${t.getFullYear()}, ${minuteLabel(t.getHours() * 60 + t.getMinutes())}`; };
  const clock = ms => { const t = new Date(ms); return minuteLabel(t.getHours() * 60 + t.getMinutes()); };
  const pct = (a, b) => (b ? Math.round(a * 100 / b) : 0);
  const range = R => R.from === R.to ? longDate(R.from)
    : (R.from.slice(0, 4) === R.to.slice(0, 4) ? `${shortDate(R.from)} – ${longDate(R.to)}` : `${longDate(R.from)} – ${longDate(R.to)}`);

  /** Times of day: sorted, each lasting until the next starts. */
  function slotsOf(R) {
    const a = (R.slots && R.slots.length ? R.slots : [{ name: 'Morning', start: 300 }, { name: 'Afternoon', start: 720 }, { name: 'Evening', start: 1020 }, { name: 'Night', start: 1200 }])
      .slice().sort((x, y) => x.start - y.start);
    return a.map((s, i) => ({ name: s.name, start: s.start, end: i + 1 < a.length ? a[i + 1].start : a[0].start + 1440 }));
  }
  const slotOf = (m, slots) => slots.find(s => (m >= s.start && m < s.end) || (m + 1440 >= s.start && m + 1440 < s.end)) || slots[0];

  /** Everything the report shows, worked out once. */
  function compute(R) {
    const rows = (R.rows || []).slice().sort((a, b) => a.scheduledAt - b.scheduledAt);
    const finals = rows.filter(r => r.final);
    const live = finals.length === 0 && rows.length > 0;           // only today, not closed yet
    const base = live ? rows : finals;
    const n = st => base.filter(r => r.status === st).length;
    const taken = n('taken'), not = n('not'), missed = n('missed'), pending = n('pending'), total = base.length;
    const answered = taken + not;
    const onTime = base.filter(r => r.status === 'taken' && r.answeredAt && r.answeredAt - r.scheduledAt <= 30 * 60000).length;
    const days = [];
    for (let d = R.from; d <= R.to; d = addDays(d, 1)) {
      const l = rows.filter(r => r.day === d);
      const c = st => l.filter(r => r.status === st).length;
      days.push({ day: d, total: l.length, taken: c('taken'), not: c('not'), missed: c('missed'), pending: c('pending'), final: l.some(r => r.final) });
    }
    const dayState = x => !x.total ? 'none' : x.pending && !x.final ? 'live' : x.missed ? 'missed' : x.not ? 'not' : 'taken';
    days.forEach(x => { x.state = dayState(x); });
    const meds = {};
    for (const r of base) {
      const k = r.name.toLowerCase(), m = meds[k] || (meds[k] = { name: r.name, color: r.color, total: 0, taken: 0, not: 0, missed: 0, pending: 0 });
      m.total++; m[r.status]++;
    }
    const slots = slotsOf(R).map(s => ({ ...s, total: 0, taken: 0, not: 0, missed: 0, pending: 0 }));
    for (const r of base) { const s = slotOf(r.minute, slots); s.total++; s[r.status]++; }
    const reasons = {};
    for (const r of base) if (r.status === 'not') { const k = r.reason || 'No reason given'; reasons[k] = (reasons[k] || 0) + 1; }
    const perfect = days.filter(x => x.final && x.total && x.taken === x.total).length;
    const counted = days.filter(x => x.final && x.total).length;
    return {
      rows, live, total, taken, not, missed, pending, answered, onTime, days,
      attendance: pct(answered, total), takenPct: pct(taken, total), onTimePct: pct(onTime, taken),
      meds: Object.values(meds).sort((a, b) => b.total - a.total || a.name.localeCompare(b.name)),
      slots: slots.filter(s => s.total),
      reasons: Object.entries(reasons).sort((a, b) => b[1] - a[1]),
      perfect, counted, single: R.from === R.to || R.period === 'overnight'
    };
  }

  /** One plain sentence that says it all. */
  function verdict(R, S) {
    const who = R.person || 'You';
    if (!S.total) return `No medicines were due in this period.`;
    if (S.live) return `So far today ${S.answered} of ${S.total} ${S.total === 1 ? 'dose is' : 'doses are'} answered: ${S.taken} taken, ${S.not} not taken.${S.pending ? ` ${S.pending} still to answer.` : ' All done for now.'}`;
    const parts = [`${S.taken} taken`];
    if (S.not) parts.push(`${S.not} not taken (with a reason)`);
    if (S.missed) parts.push(`${S.missed} with no answer`);
    return `${who} answered ${S.answered} of ${S.total} doses – ${S.attendance}%. ${parts.join(', ')}.`;
  }

  // ------------------------------------------------------------------ HTML (screen and PDF)

  function css(mode, dark, accent) {
    const P = mode === 'print';
    const c = (k) => STATUS[k][dark && !P ? 'dark' : 'light'];
    return `
.rp { --ink:${dark && !P ? '#EDEAF7' : '#1D1B2E'}; --muted:${dark && !P ? '#A9A3BF' : '#625C72'}; --card:${dark && !P ? '#1C1B36' : '#FFFFFF'};
  --paper:${dark && !P ? '#121126' : (P ? '#FFFFFF' : '#FBF7F0')}; --line:${dark && !P ? '#34325A' : '#E4DDD0'}; --soft:${dark && !P ? '#24223F' : '#F5F0E8'};
  --accent:${dark && !P ? '#A39CF2' : '#4E46B4'}; --taken:${c('taken')}; --not:${c('not')}; --missed:${c('missed')}; --pending:${c('pending')};
  font-family: Lexend, "Segoe UI", system-ui, sans-serif; color: var(--ink); background: var(--paper); line-height: 1.45; font-size: ${P ? '10.5pt' : '15px'};
  ${P ? '' : 'padding: 4px 0 120px;'} -webkit-print-color-adjust: exact; print-color-adjust: exact; }
.rp * { box-sizing: border-box; }
.rp .page > *, .rp .card, .rp .two > * { min-width: 0; }
.rp .page { max-width: ${P ? 'none' : '880px'}; margin: 0 auto; display: grid; gap: ${P ? '14px' : '18px'}; padding: 0 ${P ? '0' : '14px'}; }
.rp h1, .rp h2, .rp .serif { font-family: "Young Serif", Georgia, serif; font-weight: 400; margin: 0; }
.rp .mast { text-align: center; display: grid; gap: 2px; justify-items: center; padding-bottom: 10px; border-bottom: 3px double var(--line); }
.rp .kick { font-size: .72em; letter-spacing: .22em; text-transform: uppercase; color: var(--muted); font-weight: 600; }
.rp .mast h1 { font-size: ${P ? '24pt' : '2.1rem'}; line-height: 1.1; }
.rp .mast .sub { color: var(--muted); }
.rp .livebadge { display: inline-block; margin-top: 4px; font-size: .78em; font-weight: 600; color: var(--not); border: 1.5px solid var(--not); border-radius: 99px; padding: 1px 10px; }
.rp .card { background: var(--card); border: 1px solid var(--line); border-radius: ${P ? '10px' : '22px'}; padding: ${P ? '12px 14px' : '16px 18px'}; break-inside: avoid; }
.rp .verdict { display: grid; grid-template-columns: 1fr auto 1fr; gap: ${P ? '14px' : '18px'}; align-items: center; }
.rp .side { display: grid; gap: ${P ? '8px' : '10px'}; }
.rp .tile { border-radius: ${P ? '8px' : '16px'}; padding: ${P ? '8px 10px' : '10px 14px'}; background: var(--soft); background: color-mix(in srgb, var(--k) 10%, var(--card)); border: 1px solid var(--line); border: 1px solid color-mix(in srgb, var(--k) 35%, transparent); display: grid; grid-template-columns: 1fr auto; align-items: baseline; gap: 2px 8px; }
.rp .tile b { font-family: "Young Serif", Georgia, serif; font-weight: 400; font-size: ${P ? '20pt' : '1.9rem'}; line-height: 1; color: var(--k); }
.rp .tile span { font-weight: 600; font-size: .9em; }
.rp .tile small { grid-column: 1 / -1; color: var(--muted); font-size: .78em; }
.rp .left .tile { text-align: left; } .rp .right .tile { text-align: right; grid-template-columns: auto 1fr; }
.rp .right .tile b { order: -1; }
.rp .ring { width: ${P ? '150px' : '180px'}; height: ${P ? '150px' : '180px'}; }
.rp .sentence { grid-column: 1 / -1; text-align: center; font-size: 1.05em; margin: 2px auto 0; max-width: 60ch; }
.rp .sec { display: grid; grid-template-columns: 1fr auto 1fr; gap: 10px; align-items: center; margin: 2px 0 8px; }
.rp .sec::before, .rp .sec::after { content: ""; height: 1px; background: var(--line); }
.rp .sec h2 { font-size: ${P ? '13pt' : '1.25rem'}; text-align: center; }
.rp .sec h2 small { display: block; font-family: Lexend, system-ui, sans-serif; font-size: .62em; color: var(--muted); }
.rp .legend { display: flex; flex-wrap: wrap; justify-content: center; gap: 6px 14px; font-size: .78em; color: var(--muted); margin-top: 8px; }
.rp .legend i { display: inline-block; width: 10px; height: 10px; border-radius: 3px; margin-right: 5px; vertical-align: -1px; background: var(--k); }
.rp .legend i.o { background: none; border: 1.5px dashed var(--muted); }
.rp .cal { display: grid; gap: ${P ? '4px' : '5px'}; justify-content: center; }
.rp .cal.fluid { gap: 2px; justify-content: stretch; }
.rp .cal.fluid .cell { border-radius: 2px; width: 100%; min-width: 0; }
.rp .cal.fluid .cell.live, .rp .cal.fluid .cell.none { border: 0; outline-offset: -1px; }
.rp .cal.fluid .cell.none { outline: 1px solid var(--line); }
.rp .cal.fluid .cell.live { outline: 1.5px dashed var(--muted); }
.rp .cal .h { text-align: center; font-size: .7em; color: var(--muted); font-weight: 600; }
.rp .cell { aspect-ratio: 1; border-radius: ${P ? '5px' : '8px'}; display: grid; place-items: center; font-size: .72em; font-weight: 600; color: #fff; background: var(--k); }
.rp .cell.none { background: none; color: var(--muted); border: 1px solid var(--line); }
.rp .cell.live { background: none; color: var(--ink); border: 1.5px dashed var(--muted); }
.rp .cell.gap { background: none; border: 0; }
.rp .week { display: grid; grid-template-columns: repeat(7, 1fr); gap: ${P ? '6px' : '8px'}; }
.rp .week .day { border-radius: ${P ? '8px' : '14px'}; padding: 8px 4px; text-align: center; background: var(--soft); background: color-mix(in srgb, var(--k) 14%, var(--card)); border: 1.5px solid var(--k); }
.rp .week .day.none { --k: var(--line); background: none; }
.rp .week .day.live { --k: var(--muted); border-style: dashed; background: none; }
.rp .week .day b { display: block; font-family: "Young Serif", Georgia, serif; font-weight: 400; font-size: 1.35em; }
.rp .week .day small { display: block; color: var(--muted); font-size: .75em; }
.rp .week .day .f { display: block; font-weight: 600; color: var(--k); font-size: .85em; margin-top: 2px; }
.rp .timeline { position: relative; height: ${P ? '64px' : '76px'}; margin: 6px 6px 0; }
.rp .timeline .axis { position: absolute; left: 0; right: 0; top: 30px; height: 2px; background: var(--line); }
.rp .timeline .tick { position: absolute; top: 40px; transform: translateX(-50%); font-size: .7em; color: var(--muted); white-space: nowrap; }
.rp .timeline .dot { position: absolute; top: 22px; width: 18px; height: 18px; border-radius: 50%; transform: translateX(-50%); background: var(--k); border: 2px solid var(--card); box-shadow: 0 0 0 1.5px var(--k); }
.rp .timeline .dot.pending { background: var(--card); }
.rp .two { display: grid; grid-template-columns: 1fr 1fr; gap: ${P ? '12px' : '16px'}; }
.rp .bar { height: ${P ? '10px' : '12px'}; border-radius: 99px; background: var(--soft); overflow: hidden; display: flex; }
.rp .bar i { display: block; height: 100%; background: var(--k); }
.rp .brow { display: grid; grid-template-columns: minmax(0, 7.5em) 1fr auto; gap: 10px; align-items: center; margin: 7px 0; font-size: .9em; }
.rp .brow .n { font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rp .brow .v { color: var(--muted); font-variant-numeric: tabular-nums; min-width: 3.4em; text-align: right; }
.rp .reason { display: grid; grid-template-columns: 1fr auto; gap: 8px; padding: 6px 0; border-bottom: 1px dashed var(--line); font-size: .9em; }
.rp .reason:last-child { border-bottom: 0; }
.rp .reason b { color: var(--not); }
.rp .empty { color: var(--muted); text-align: center; padding: 10px 0; font-size: .9em; }
.rp table { width: 100%; border-collapse: collapse; font-size: .88em; font-variant-numeric: tabular-nums; }
.rp th { text-align: left; font-size: .78em; letter-spacing: .06em; text-transform: uppercase; color: var(--muted); font-weight: 600; padding: 6px 6px; border-bottom: 1.5px solid var(--line); }
.rp td { padding: 7px 6px; border-bottom: 1px solid var(--line); vertical-align: middle; }
.rp tr { break-inside: avoid; }
.rp td.c, .rp th.c { text-align: center; }
.rp .dotc { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 8px; vertical-align: 0; }
.rp .chip { display: inline-block; font-size: .8em; font-weight: 600; padding: 1px 9px; border-radius: 99px; color: var(--k); background: var(--soft); background: color-mix(in srgb, var(--k) 13%, transparent); white-space: nowrap; }
.rp .mbar { width: 100%; min-width: 70px; }
.rp .dayhead td { background: var(--soft); font-weight: 600; font-size: .85em; }
.rp .foot { text-align: center; color: var(--muted); font-size: .76em; border-top: 3px double var(--line); padding-top: 8px; }
.rp .scroll { overflow-x: auto; }
@media (max-width: 560px) {
  .rp .verdict { grid-template-columns: 1fr 1fr; } .rp .ringbox { grid-column: 1 / -1; order: -1; justify-self: center; }
  .rp .right .tile { text-align: left; grid-template-columns: 1fr auto; } .rp .right .tile b { order: 0; }
  .rp .two { grid-template-columns: 1fr; } .rp .week { gap: 4px; } .rp .week .day small { display: none; }
}
${P ? '@page { size: A4; margin: 14mm 13mm; } .rp .page { gap: 12px; } .rp .card { box-shadow: none; }' : ''}`;
  }

  function ringSVG(S) {
    const r1 = 62, r2 = 46, C1 = 2 * Math.PI * r1, C2 = 2 * Math.PI * r2;
    const a = S.total ? S.answered / S.total : 0, t = S.total ? S.taken / S.total : 0;
    return `<svg class="ring" viewBox="0 0 150 150" role="img" aria-label="Attendance ${S.attendance} percent">
      <circle cx="75" cy="75" r="${r1}" fill="none" stroke="var(--soft)" stroke-width="11"/>
      <circle cx="75" cy="75" r="${r1}" fill="none" stroke="var(--accent)" stroke-width="11" stroke-linecap="round" stroke-dasharray="${(C1 * a).toFixed(1)} ${C1.toFixed(1)}" transform="rotate(-90 75 75)"/>
      <circle cx="75" cy="75" r="${r2}" fill="none" stroke="var(--soft)" stroke-width="9"/>
      <circle cx="75" cy="75" r="${r2}" fill="none" stroke="var(--taken)" stroke-width="9" stroke-linecap="round" stroke-dasharray="${(C2 * t).toFixed(1)} ${C2.toFixed(1)}" transform="rotate(-90 75 75)"/>
      <text x="75" y="76" text-anchor="middle" font-family="Young Serif, Georgia, serif" font-size="27" fill="var(--ink)">${S.total ? S.attendance + '%' : '–'}</text>
      <text x="75" y="95" text-anchor="middle" font-family="Lexend, sans-serif" font-size="9.5" fill="var(--muted)">${S.live ? 'answered so far' : 'attendance'}</text></svg>`;
  }

  const tile = (k, n, label, note) => `<div class="tile" style="--k:var(--${k})"><span>${label}</span><b>${n}</b><small>${note}</small></div>`;

  function daysBlock(R, S) {
    const legend = `<div class="legend"><span><i style="--k:var(--taken)"></i>All taken</span><span><i style="--k:var(--not)"></i>Some not taken</span><span><i style="--k:var(--missed)"></i>Some not answered</span><span><i class="o"></i>No medicines / not closed yet</span></div>`;
    if (S.single) {
      // a single day (or overnight): the day's timeline
      const over = R.period === 'overnight';
      const lo = over ? -240 : 0, hi = over ? 480 : 1440;
      const pos = r => { let m = r.minute; if (over && r.day !== R.to) m -= 1440; return ((m - lo) / (hi - lo) * 100).toFixed(2); };
      const ticks = over ? [[-240, '8 PM'], [0, '12 AM'], [240, '4 AM'], [480, '8 AM']] : [[0, '12 AM'], [360, '6 AM'], [720, 'Noon'], [1080, '6 PM'], [1440, '12 AM']];
      return `<div class="card"><div class="sec"><h2>The day<small>${over ? '8 PM to 8 AM' : 'Every dose at its time'}</small></h2></div>
        <div class="timeline"><div class="axis"></div>
        ${ticks.map(([m, t], i) => `<span class="tick" style="left:${((m - lo) / (hi - lo) * 100).toFixed(2)}%;${i === 0 ? 'transform:none' : i === ticks.length - 1 ? 'transform:translateX(-100%)' : ''}">${t}</span>`).join('')}
        ${S.rows.map(r => `<span class="dot ${r.status}" style="left:${pos(r)}%;--k:var(--${r.status})" title="${esc(r.name)} ${minuteLabel(r.minute)}"></span>`).join('')}</div>
        <div class="legend"><span><i style="--k:var(--taken)"></i>Taken</span><span><i style="--k:var(--not)"></i>Not taken</span><span><i style="--k:var(--missed)"></i>No answer</span><span><i class="o"></i>Still to answer</span></div></div>`;
    }
    if (S.days.length <= 7) {
      return `<div class="card"><div class="sec"><h2>Day by day<small>Answered out of due</small></h2></div><div class="week">${S.days.map(x => {
        const t = parse(x.day);
        return `<div class="day ${x.state}" style="--k:var(--${x.state === 'none' || x.state === 'live' ? 'muted' : x.state})"><small>${WD[dow(x.day)]}</small><b>${t.getDate()}</b><span class="f">${x.total ? `${x.taken + x.not}/${x.total}` : '–'}</span></div>`;
      }).join('')}</div>${legend}</div>`;
    }
    // a calendar: 7 columns, Monday first
    const lead = dow(S.days[0].day);
    const big = S.days.length <= 31;
    const cells = Array(lead).fill('<span class="cell gap"></span>').concat(S.days.map(x => {
      const t = parse(x.day);
      return `<span class="cell ${x.state}" style="--k:var(--${x.state})" title="${longDate(x.day)}: ${x.taken + x.not} of ${x.total} answered">${big ? t.getDate() : ''}</span>`;
    }));
    const width = big ? (R.mode === 'print' ? 34 : 40) : (R.mode === 'print' ? 15 : 16);
    if (big) {
      return `<div class="card"><div class="sec"><h2>Day by day<small>${shortDate(R.from)} – ${shortDate(R.to)}</small></h2></div>
        <div class="cal" style="grid-template-columns:repeat(7,${width}px)">${WD.map(w => `<span class="h">${w[0]}</span>`).join('')}${cells.join('')}</div>${legend}</div>`;
    }
    // long periods: weeks run left to right, Monday on top (like a year planner)
    const weeks = Math.ceil((lead + S.days.length) / 7);
    const grid = [];
    for (let d = 0; d < 7; d++) for (let w = 0; w < weeks; w++) grid.push(cells[w * 7 + d] || '<span class="cell gap"></span>');
    const months = [];
    for (let w = 0; w < weeks; w++) {
      const i = Math.max(0, w * 7 - lead), x = S.days[i];
      const m = x ? parse(x.day).getMonth() : -1;
      months.push(x && (w === 0 || m !== parse(S.days[Math.max(0, (w - 1) * 7 - lead)].day).getMonth()) ? MONTHS[m].slice(0, 3) : '');
    }
    return `<div class="card"><div class="sec"><h2>Day by day<small>${S.counted} days with medicines · ${S.perfect} perfect days</small></h2></div>
      <div class="cal fluid" style="grid-template-columns:2.4em repeat(${weeks},minmax(4px,1fr))">
      <span></span>${months.map(m => `<span class="h" style="text-align:left;white-space:nowrap;overflow:visible">${m}</span>`).join('')}
      ${WD.map((w, d) => `<span class="h" style="text-align:right;padding-right:4px">${d % 2 ? '' : w}</span>` + grid.slice(d * weeks, d * weeks + weeks).join('')).join('')}
      </div>${legend}</div>`;
  }

  function stackBar(x) {
    const t = x.total || 1;
    return `<span class="bar">${['taken', 'not', 'missed', 'pending'].map(k => x[k] ? `<i style="--k:var(--${k});width:${(x[k] / t * 100).toFixed(1)}%"></i>` : '').join('')}</span>`;
  }

  function html(R, mode = 'screen', opts = {}) {
    R = { ...R, mode };
    const S = compute(R), colors = R.colors && R.colors.length ? R.colors : MED_COLORS;
    const pendingLabel = S.live ? 'still to answer' : 'no answer';
    const out = [];
    out.push(`<style>${css(mode, !!opts.dark, opts.accent)}</style><div class="rp"><div class="page">`);
    // 1. masthead
    out.push(`<header class="mast"><span class="kick">Ragini · medicine report</span><h1>${esc(R.title)}</h1>
      <div class="sub">${R.person ? `For ${esc(R.person)} · ` : ''}${range(R)}</div>${S.live ? '<span class="livebadge">Live – today is not closed yet</span>' : ''}</header>`);
    // 2. verdict
    const missedK = S.live ? 'pending' : 'missed', missedN = S.live ? S.pending : S.missed;
    out.push(`<section class="card verdict">
      <div class="side left">${tile('taken', S.taken, 'Taken', `${pct(S.taken, S.total)}% of doses`)}${tile('not', S.not, 'Not taken', 'with a reason – still counts as answered')}</div>
      <div class="ringbox">${ringSVG(S)}</div>
      <div class="side right">${tile(missedK, missedN, S.live ? 'To answer' : 'No answer', `${pct(missedN, S.total)}% of doses`)}${
        S.single ? tile('accent', S.taken ? S.onTimePct + '%' : '–', 'On time', 'taken within 30 minutes')
        : tile('accent', S.perfect, 'Perfect days', `of ${S.counted} · streak ${R.streak ? R.streak.current : 0}, best ${R.streak ? R.streak.best : 0}`)}</div>
      <p class="sentence">${esc(verdict(R, S))}</p></section>`);
    if (!S.total) {
      out.push(`<div class="card empty">There is nothing to show for these dates yet. Attendance is written at ${esc(R.closeLabel || '11:59 PM')} each night.</div>`);
    } else {
      // 3. days
      out.push(daysBlock(R, S));
      // 4. time of day | reasons
      out.push(`<div class="two"><section class="card"><div class="sec"><h2>Time of day<small>Your own times of day</small></h2></div>
        ${S.slots.map(s => `<div class="brow"><span class="n">${esc(s.name)}</span>${stackBar(s)}<span class="v">${s.taken}/${s.total}</span></div>`).join('') || '<div class="empty">–</div>'}</section>
        <section class="card"><div class="sec"><h2>Reasons<small>When a dose was not taken</small></h2></div>
        ${S.reasons.length ? S.reasons.map(([r, n]) => `<div class="reason"><span>${esc(r)}</span><b>×${n}</b></div>`).join('') : '<div class="empty">No dose was skipped. Well done!</div>'}</section></div>`);
      // 5. medicines
      out.push(`<section class="card"><div class="sec"><h2>Medicines<small>${S.meds.length} ${S.meds.length === 1 ? 'medicine' : 'medicines'}</small></h2></div><div class="scroll"><table>
        <thead><tr><th>Medicine</th><th class="c">Due</th><th class="c">Taken</th><th class="c">Not taken</th><th class="c">${S.live ? 'To answer' : 'No answer'}</th><th style="width:28%">Taken</th></tr></thead><tbody>
        ${S.meds.map(m => `<tr><td><span class="dotc" style="background:${colors[m.color % colors.length]}"></span>${esc(m.name)}</td><td class="c">${m.total}</td><td class="c">${m.taken}</td><td class="c">${m.not}</td><td class="c">${S.live ? m.pending : m.missed}</td>
          <td><div class="brow" style="grid-template-columns:1fr auto;margin:0"><span class="mbar">${stackBar(m)}</span><span class="v">${pct(m.taken, m.total)}%</span></div></td></tr>`).join('')}
        </tbody></table></div></section>`);
      // 6. register
      const long = S.rows.length > 400 && mode !== 'print';
      const regRows = long ? S.rows.slice(-400) : S.rows;
      let lastDay = '';
      out.push(`<section class="card"><div class="sec"><h2>Attendance register<small>${long ? 'The latest 400 doses – the spreadsheet has all of them' : 'Every dose, in order'}</small></h2></div><div class="scroll"><table>
        <thead><tr><th>Time</th><th>Medicine</th><th>Answer</th><th>Reason</th><th>Answered at</th></tr></thead><tbody>
        ${regRows.map(r => {
          const head = !S.single && r.day !== lastDay ? `<tr class="dayhead"><td colspan="5">${WD[dow(r.day)]}, ${longDate(r.day)}</td></tr>` : '';
          lastDay = r.day;
          const st = STATUS[r.status] || STATUS.pending;
          return `${head}<tr><td>${minuteLabel(r.minute)}</td><td><span class="dotc" style="background:${colors[r.color % colors.length]}"></span>${esc(r.name)}</td>
            <td><span class="chip" style="--k:var(--${r.status})">${st.label}</span></td><td>${esc(r.status === 'not' ? r.reason : '')}</td><td>${r.answeredAt ? clock(r.answeredAt) : '–'}</td></tr>`;
        }).join('')}</tbody></table></div></section>`);
    }
    out.push(`<footer class="foot">Attendance counts every dose that was answered – Taken, or Not taken with a reason. Each day is closed and written in the register at ${esc(R.closeLabel || '11:59 PM')}.<br>Made by Ragini medicine reminder on ${stamp(R.madeAt || Date.now())}.</footer>`);
    out.push('</div></div>');
    return out.join('');
  }

  // ------------------------------------------------------------------ spreadsheet (CSV) and text

  function csv(R) {
    const S = compute(R), q = v => `"${String(v ?? '').replace(/"/g, '""')}"`, L = [];
    const row = (...a) => L.push(a.map(q).join(','));
    row('RAGINI MEDICINE REPORT'); row(R.title, R.person ? `For ${R.person}` : ''); row('Dates', range(R)); row('Made on', stamp(R.madeAt || Date.now()));
    if (S.live) row('Note', 'Live – today is not closed yet');
    row(); row('SUMMARY'); row('', 'Doses', 'Share');
    row('Attendance (answered)', `${S.answered} of ${S.total}`, `${S.attendance}%`);
    row('Taken', S.taken, `${pct(S.taken, S.total)}%`); row('Not taken (with a reason)', S.not, `${pct(S.not, S.total)}%`);
    row(S.live ? 'Still to answer' : 'No answer', S.live ? S.pending : S.missed, `${pct(S.live ? S.pending : S.missed, S.total)}%`);
    row('Taken on time (within 30 min)', S.onTime, `${S.onTimePct}%`);
    if (!S.single) row('Perfect days', `${S.perfect} of ${S.counted}`, '');
    row('In one sentence', verdict(R, S));
    row(); row('BY MEDICINE'); row('Medicine', 'Due', 'Taken', 'Not taken', S.live ? 'Still to answer' : 'No answer', 'Taken %');
    S.meds.forEach(m => row(m.name, m.total, m.taken, m.not, S.live ? m.pending : m.missed, `${pct(m.taken, m.total)}%`));
    row(); row('BY TIME OF DAY'); row('Time of day', 'Due', 'Taken', 'Not taken', S.live ? 'Still to answer' : 'No answer', 'Taken %');
    S.slots.forEach(s => row(s.name, s.total, s.taken, s.not, S.live ? s.pending : s.missed, `${pct(s.taken, s.total)}%`));
    row(); row('REASONS FOR NOT TAKING'); row('Reason', 'Times');
    if (S.reasons.length) S.reasons.forEach(([r, n]) => row(r, n)); else row('No dose was skipped', 0);
    if (!S.single) { row(); row('DAY BY DAY'); row('Date', 'Day', 'Due', 'Taken', 'Not taken', 'No answer', 'Answered %'); S.days.forEach(x => row(x.day, WD[dow(x.day)], x.total, x.taken, x.not, x.missed, x.total ? `${pct(x.taken + x.not, x.total)}%` : '')); }
    const slots = slotsOf(R);
    row(); row('ATTENDANCE REGISTER'); row('Date', 'Day', 'Time', 'Time of day', 'Medicine', 'Answer', 'Reason', 'Answered at');
    S.rows.forEach(r => row(r.day, WD[dow(r.day)], minuteLabel(r.minute), slotOf(r.minute, slots).name, r.name, (STATUS[r.status] || STATUS.pending).label, r.status === 'not' ? r.reason : '', r.answeredAt ? stamp(r.answeredAt) : ''));
    return '﻿' + L.join('\r\n');
  }

  function text(R) {
    const S = compute(R), L = [];
    L.push(`💊 RAGINI · ${R.title.toUpperCase()}`);
    L.push(`${R.person ? `For ${R.person} · ` : ''}${range(R)}`);
    if (S.live) L.push('(Live – today is not closed yet)');
    L.push('━━━━━━━━━━━━━━━━');
    if (!S.total) { L.push('No medicines were due in this period.'); return L.join('\n'); }
    L.push(`Attendance: ${S.attendance}%  (${S.answered} of ${S.total} answered)`);
    L.push(`✅ Taken ${S.taken}   ⚠️ Not taken ${S.not}   ${S.live ? `⏳ To answer ${S.pending}` : `❌ No answer ${S.missed}`}`);
    L.push('', verdict(R, S));
    if (!S.single && S.days.length <= 31) {
      L.push('', 'Day by day:');
      L.push(S.days.map(x => x.state === 'taken' ? '🟩' : x.state === 'not' ? '🟨' : x.state === 'missed' ? '🟥' : '⬜').join(''));
    }
    if (S.meds.length) { L.push('', 'Medicines:'); S.meds.forEach(m => L.push(`• ${m.name}: ${m.taken}/${m.total} taken${m.not ? `, ${m.not} not taken` : ''}${!S.live && m.missed ? `, ${m.missed} no answer` : ''}`)); }
    if (S.reasons.length) { L.push('', 'Reasons for not taking:'); S.reasons.forEach(([r, n]) => L.push(`• ${r} ×${n}`)); }
    L.push('━━━━━━━━━━━━━━━━', 'Sent from Ragini medicine reminder');
    return L.join('\n');
  }

  return { html, csv, text, compute, range };
})();

if (typeof module !== 'undefined') module.exports = RaginiReport;
