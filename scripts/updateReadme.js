const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const readmeFile = path.join(__dirname, '..', 'README.md');
let readme = '';
if (fs.existsSync(readmeFile)) {
  readme = fs.readFileSync(readmeFile, 'utf8');
}

const log = execSync('git log -1 --pretty=format:"%H||%ad||%s"').toString().trim();
const [hash, date, msg] = log.split('||');

const entry = `\n### ${date}\n- ${msg} (${hash})\n`;
fs.writeFileSync(readmeFile, readme + entry);
