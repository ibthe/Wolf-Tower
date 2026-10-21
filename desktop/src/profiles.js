const fs = require('fs');

// Имена стратегий -> аргументы ByeDPI. Те же, что в Android (StrategyArgs.kt).
const LIBRARY = {
  split: ['-s1+s'],
  disorder: ['-d1+s'],
  oob: ['-o1+s'],
  disoob: ['-q1+s'],
  'split+tlsrec': ['-s1+s', '-r1+s'],
  'disorder+tlsrec': ['-d1+s', '-r1+s'],
  'oob+tlsrec': ['-o1+s', '-r1+s'],
  'fake+disorder': ['-f-1', '-t', '8', '-d1+s'],
  'fake+oob': ['-f-1', '-t', '8', '-o1+s'],
  'fake+split': ['-f-1', '-t', '8', '-s1+s'],
  'fake+tlsrec': ['-f-1', '-t', '8', '-r1+s'],
  'fake4+disorder': ['-f-1', '-t', '4', '-d1+s'],
  'fake12+disorder': ['-f-1', '-t', '12', '-d1+s'],
};

/** "none" и неизвестные имена = без обхода. */
function forName(name) {
  return LIBRARY[name] || [];
}

/**
 * Своя стратегия для каждого сервиса: группы опций ByeDPI, разделённые --auto=none.
 * Группа по доменам срабатывает по SNI; для сервисов с ip_ranges (Telegram) добавляется
 * вторая группа по IP-диапазонам. selection: [{ profile, strategy }]
 */
function buildGroups(selection) {
  const out = [];
  const group = (flag, value, strategy) => {
    if (out.length) out.push('--auto=none');
    out.push(flag, ':' + value, ...forName(strategy));
  };
  for (const { profile, strategy } of selection) {
    if (strategy === 'none') continue;
    if (profile.domains.length) group('--hosts', profile.domains.join(' '), strategy);
    if (profile.ipRanges.length) group('--ipset', profile.ipRanges.join(' '), strategy);
  }
  return out;
}

function parseConfig(json) {
  const profiles = (json.profiles || []).map((o) => ({
    id: o.id,
    name: o.name,
    domains: o.domains || [],
    ipRanges: o.ip_ranges || [],
    probeHosts: o.probe_hosts || [],
    blockQuic: !!o.block_quic,
  }));
  const strategies = json.strategies || [];
  const defaultStrategy = strategies.includes('disorder') ? 'disorder' : strategies[0];
  return { profiles, strategies, defaultStrategy };
}

function loadConfig(paths) {
  for (const p of paths) {
    try {
      return parseConfig(JSON.parse(fs.readFileSync(p, 'utf8')));
    } catch (_) {
      /* пробуем следующий путь */
    }
  }
  throw new Error('Не найден profiles.json');
}

module.exports = { LIBRARY, forName, buildGroups, parseConfig, loadConfig };
