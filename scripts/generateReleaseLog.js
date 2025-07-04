const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const logFile = path.join(__dirname, '..', 'RELEASE_LOG.md');
let lastCommit = '';
if (fs.existsSync(logFile)) {
  const content = fs.readFileSync(logFile, 'utf8');
  const match = content.trim().split('\n').reverse().find(line => line.startsWith('commit '));
  if (match) {
    lastCommit = match.split(' ')[1];
  }
}

let cmd = 'git log --pretty=format:"%H||%an||%ad||%s"';
if (lastCommit) {
  cmd += ` ${lastCommit}..HEAD`;
}
const logs = execSync(cmd, { encoding: 'utf8' }).trim();
if (!logs) process.exit(0);

const entries = logs.split('\n').reverse();
let output = '';
entries.forEach(line => {
  const [hash, author, date, msg] = line.split('||');
  output += `commit ${hash}\nAuthor: ${author}\nDate:   ${date}\nMessage: ${msg}\n\n`;
});

fs.appendFileSync(logFile, output);
