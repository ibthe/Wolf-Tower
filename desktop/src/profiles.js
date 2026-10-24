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
  'multisplit-short': ['-d1', '-s1+s', '-s3+s', '-s6+s'],
  'multisplit': ['-d1', '-s1+s', '-s3+s', '-s6+s', '-s9+s', '-s12+s', '-s15+s', '-s20+s', '-s30+s', '-a1'],
  'multidisorder': ['-d1', '-d3+s', '-d6+s', '-d9+s', '-d12+s', '-d15+s', '-d20+s', '-d30+s'],
};

// Имя для фальшивых пакетов (подставляется вместо {sni} в наборах); можно переопределить в profiles.json (fake_sni)
let fakeSni = 'www.iana.org';

/** "none" и неизвестные имена = без обхода. */
function forName(name) {
  const raw = LIBRARY[name];
  return raw ? raw.map((x) => x.replace('{sni}', fakeSni)) : [];
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
  // Описания стратегий лежат в profiles.json: новые наборы добавляются без правки кода
  if (json.strategy_args) Object.assign(LIBRARY, json.strategy_args);
  if (json.fake_sni) fakeSni = json.fake_sni;
  const profiles = (json.profiles || []).map((o) => ({
    id: o.id,
    name: o.name,
    domains: o.domains || [],
    ipRanges: o.ip_ranges || [],
    probeHosts: o.probe_hosts || [],
    blockQuic: !!o.block_quic,
  }));
  const strategies = json.strategies || [];
  // Расширенные наборы: в ручном выборе не показываются, но проверяются автоподбором
  const extended = json.strategies_extended || [];
  const defaultStrategy = strategies.includes('disorder') ? 'disorder' : strategies[0];
  return { profiles, strategies, extended, allStrategies: [...strategies, ...extended], defaultStrategy };
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
