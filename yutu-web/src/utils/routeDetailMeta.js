const META_MARKER = "\n\n[[YUTU_ROUTE_META]]\n";

function toUniqueImageList(input) {
  const result = [];
  (Array.isArray(input) ? input : []).forEach((item) => {
    const url = String(item ?? "").trim();
    if (!url) return;
    if (!result.includes(url)) result.push(url);
  });
  return result;
}

export function parseRouteDetailContent(content) {
  const raw = String(content ?? "");
  const idx = raw.indexOf(META_MARKER);
  if (idx < 0) {
    return {
      plainText: raw.trim(),
      relatedImages: []
    };
  }

  const plainText = raw.slice(0, idx).trim();
  const metaRaw = raw.slice(idx + META_MARKER.length).trim();
  try {
    const meta = JSON.parse(metaRaw);
    return {
      plainText,
      relatedImages: toUniqueImageList(meta?.relatedImages).slice(0, 4)
    };
  } catch (e) {
    return {
      plainText: raw.trim(),
      relatedImages: []
    };
  }
}

export function buildRouteDetailContent(plainText, relatedImages) {
  const text = String(plainText ?? "").trim();
  const images = toUniqueImageList(relatedImages).slice(0, 4);
  if (!images.length) return text;
  return `${text}${META_MARKER}${JSON.stringify({ relatedImages: images })}`;
}
