/**
 * Autoroid Landing Page JavaScript
 * ScoRpiiTech - 2026
 */

document.addEventListener('DOMContentLoaded', () => {
  fetchLatestRelease();
  initFaqAccordion();
});

/**
 * Fetch latest release APK metadata from GitHub Releases API
 */
async function fetchLatestRelease() {
  const repo = 'ScoRpiiTech/autoroid';
  const apiUrl = `https://api.github.com/repos/${repo}/releases/latest`;
  const downloadBtn = document.getElementById('primary-download-btn');
  const versionTag = document.getElementById('release-version-tag');

  try {
    const response = await fetch(apiUrl);
    if (!response.ok) return;

    const data = await response.json();
    const tagName = data.tag_name || 'v1.3.0';

    // Find the release APK asset
    const apkAsset = data.assets?.find(asset => asset.name.endsWith('.apk'));

    if (apkAsset && downloadBtn) {
      downloadBtn.href = apkAsset.browser_download_url;
      const sizeMb = (apkAsset.size / (1024 * 1024)).toFixed(1);

      if (versionTag) {
        versionTag.textContent = `${tagName} • ${sizeMb} MB APK • Direct Download`;
      }
    } else if (downloadBtn) {
      downloadBtn.href = data.html_url || `https://github.com/${repo}/releases/latest`;
      if (versionTag) {
        versionTag.textContent = `${tagName} • Release APK • 100% Free`;
      }
    }
  } catch (err) {
    console.warn('Could not fetch latest release info:', err);
  }
}

/**
 * Switch setup guide tabs (Shizuku vs Root)
 */
function switchTab(tabId) {
  const tabs = document.querySelectorAll('.tab-pane');
  const buttons = document.querySelectorAll('.tab-btn');

  tabs.forEach(tab => {
    tab.classList.remove('active');
  });

  buttons.forEach(btn => {
    btn.classList.remove('active');
  });

  const selectedTab = document.getElementById(`tab-${tabId}`);
  if (selectedTab) {
    selectedTab.classList.add('active');
  }

  // Update active button state
  if (tabId === 'shizuku' && buttons[0]) {
    buttons[0].classList.add('active');
  } else if (tabId === 'root' && buttons[1]) {
    buttons[1].classList.add('active');
  }
}

// Attach globally for inline onclick
window.switchTab = switchTab;

/**
 * Optional: Single-open accordion behavior for FAQ
 */
function initFaqAccordion() {
  const items = document.querySelectorAll('.faq-item');
  items.forEach(item => {
    item.addEventListener('toggle', () => {
      if (item.open) {
        items.forEach(other => {
          if (other !== item) {
            other.removeAttribute('open');
          }
        });
      }
    });
  });
}
