const reducedMotionQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
const reducedMotion = reducedMotionQuery.matches;
const topicSections = Array.from(document.querySelectorAll("[data-topic-section]"));
const topicLinks = Array.from(document.querySelectorAll("[data-topic-link]"));
const topicNav = document.querySelector("[data-topic-nav]");

function setActiveTopic(sectionId) {
  topicLinks.forEach((link) => {
    const active = link.dataset.topicLink === sectionId;
    link.classList.toggle("is-active", active);
    if (active) {
      link.setAttribute("aria-current", "true");
    } else {
      link.removeAttribute("aria-current");
    }
  });

  if (topicNav) {
    topicNav.dataset.activeTopic = sectionId;
  }
}

if ("IntersectionObserver" in window) {
  const topicObserver = new IntersectionObserver((entries) => {
    const current = entries
      .filter((entry) => entry.isIntersecting)
      .sort((first, second) => second.intersectionRatio - first.intersectionRatio)[0];

    if (current?.target.id) {
      setActiveTopic(current.target.id);
    }
  }, {
    rootMargin: "-22% 0px -48% 0px",
    threshold: [0.05, 0.2, 0.45, 0.7]
  });

  topicSections.forEach((section) => topicObserver.observe(section));
}

topicLinks.forEach((link) => {
  link.addEventListener("click", () => {
    const target = link.dataset.topicLink;
    if (target) setActiveTopic(target);
  });
});

window.addEventListener("load", () => {
  const sectionId = decodeURIComponent(window.location.hash.slice(1));
  const section = document.getElementById(sectionId);
  if (!section?.matches("[data-topic-section]")) return;

  window.requestAnimationFrame(() => {
    section.scrollIntoView({ block: "start", behavior: "auto" });
  });
}, { once: true });

function markFallback(stage) {
  stage?.classList.remove("is-webgl-ready");
  stage?.classList.add("is-webgl-fallback");
}

function supportsWebGL() {
  try {
    const canvas = document.createElement("canvas");
    return Boolean(canvas.getContext("webgl2") || canvas.getContext("webgl"));
  } catch {
    return false;
  }
}

function createStageController(stage, renderFrame, staticFrame = false, onFirstVisible) {
  let frameId = 0;
  let visible = false;
  let disposed = false;
  let enteredViewport = false;

  if (staticFrame) {
    renderFrame(performance.now());
    return {
      renderOnce: renderFrame,
      dispose() {
        disposed = true;
      }
    };
  }

  const tick = (time) => {
    frameId = 0;
    if (disposed || !visible || document.hidden) return;
    renderFrame(time);
    frameId = window.requestAnimationFrame(tick);
  };

  const start = () => {
    if (!disposed && visible && !frameId && !document.hidden) {
      frameId = window.requestAnimationFrame(tick);
    }
  };

  const stop = () => {
    if (frameId) {
      window.cancelAnimationFrame(frameId);
      frameId = 0;
    }
  };

  if ("IntersectionObserver" in window) {
    const observer = new IntersectionObserver(([entry]) => {
      visible = entry.isIntersecting;
      if (visible) {
        if (!enteredViewport) {
          enteredViewport = true;
          onFirstVisible?.();
        }
        start();
      }
      else stop();
    }, { rootMargin: "180px 0px", threshold: 0.01 });
    observer.observe(stage);
  } else {
    visible = true;
    enteredViewport = true;
    onFirstVisible?.();
    start();
  }

  document.addEventListener("visibilitychange", () => {
    if (document.hidden) stop();
    else start();
  });

  return {
    renderOnce: renderFrame,
    dispose() {
      disposed = true;
      stop();
    }
  };
}

function createRenderer(THREE, canvas) {
  const renderer = new THREE.WebGLRenderer({
    canvas,
    alpha: true,
    antialias: true,
    powerPreference: "high-performance"
  });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.75));
  renderer.setClearColor(0x000000, 0);
  return renderer;
}

function observeRendererSize(THREE, stage, renderer, camera, renderOnce) {
  const resize = () => {
    const canvas = renderer.domElement;
    const width = Math.max(1, canvas.clientWidth || stage.clientWidth);
    const height = Math.max(1, canvas.clientHeight || stage.clientHeight);
    renderer.setSize(width, height, false);
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderOnce(performance.now());
  };

  const observer = new ResizeObserver(resize);
  observer.observe(stage);
  observer.observe(renderer.domElement);
  resize();
  return observer;
}

function initEcosystem(THREE) {
  const canvas = document.querySelector("[data-ecosystem-canvas]");
  const stage = canvas?.closest("[data-webgl-stage]");
  if (!canvas || !stage) return;

  try {
    const renderer = createRenderer(THREE, canvas);
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(36, 1, 0.1, 100);
    camera.position.set(0, 0.05, 7.2);

    const compact = window.matchMedia("(max-width: 680px)").matches;
    const particleCount = compact ? 900 : 1800;
    const positions = new Float32Array(particleCount * 3);
    const starts = new Float32Array(particleCount * 3);
    const targets = new Float32Array(particleCount * 3);
    const clusterTargets = new Float32Array(particleCount * 3);
    const colors = new Float32Array(particleCount * 3);
    const cool = new THREE.Color(0x087da9);
    const light = new THREE.Color(0x8dd9e8);

    for (let index = 0; index < particleCount; index += 1) {
      const offset = index * 3;
      const startX = (Math.random() - 0.5) * 7.8;
      const startY = -2.65 + Math.pow(Math.random(), 1.8) * 1.9;
      const startZ = (Math.random() - 0.5) * 2.4;
      const around = Math.random() * Math.PI * 2;
      const tube = Math.random() * Math.PI * 2;
      const majorRadius = 2.18 + (Math.random() - 0.5) * 0.12;
      const minorRadius = 0.5 + Math.random() * 0.18;
      const targetX = (majorRadius + minorRadius * Math.cos(tube)) * Math.cos(around);
      const targetY = (majorRadius + minorRadius * Math.cos(tube)) * Math.sin(around) * 0.72;
      const targetZ = minorRadius * Math.sin(tube) * 0.75;
      const clusterIndex = index % 12;
      const clusterAngle = (clusterIndex / 12) * Math.PI * 2;
      const clusterSpread = 0.18 + Math.random() * 0.24;
      const clusterAround = Math.random() * Math.PI * 2;
      const clusterX = Math.cos(clusterAngle) * 2.45 + Math.cos(clusterAround) * clusterSpread;
      const clusterY = Math.sin(clusterAngle) * 1.72 + Math.sin(clusterAround) * clusterSpread;
      const clusterZ = (Math.random() - 0.5) * 0.72;

      starts.set([startX, startY, startZ], offset);
      clusterTargets.set([clusterX, clusterY, clusterZ], offset);
      targets.set([targetX, targetY, targetZ], offset);
      positions.set(reducedMotion ? [targetX, targetY, targetZ] : [startX, startY, startZ], offset);

      const color = cool.clone().lerp(light, Math.random() * 0.72);
      colors.set([color.r, color.g, color.b], offset);
    }

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
    geometry.setAttribute("color", new THREE.BufferAttribute(colors, 3));
    const material = new THREE.PointsMaterial({
      size: compact ? 0.045 : 0.035,
      transparent: true,
      opacity: 0.82,
      vertexColors: true,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
      sizeAttenuation: true
    });
    const particles = new THREE.Points(geometry, material);
    particles.rotation.x = -0.08;
    scene.add(particles);

    const inboundCount = compact ? 120 : 240;
    const inboundPositions = new Float32Array(inboundCount * 3);
    const inboundAlphas = new Float32Array(inboundCount);
    const inboundAngles = new Float32Array(inboundCount);
    const inboundRadii = new Float32Array(inboundCount);
    const inboundDepths = new Float32Array(inboundCount);
    const inboundTwists = new Float32Array(inboundCount);
    const inboundDelays = new Float32Array(inboundCount);

    for (let index = 0; index < inboundCount; index += 1) {
      const serviceIndex = index % 12;
      const batchIndex = Math.floor(index / 12);
      const angle = (serviceIndex / 12) * Math.PI * 2 + (Math.random() - 0.5) * 0.075;
      const radius = 2.82 + Math.random() * 0.44;
      const depth = (Math.random() - 0.5) * 0.62;

      inboundAngles[index] = angle;
      inboundRadii[index] = radius;
      inboundDepths[index] = depth;
      inboundTwists[index] = (Math.random() - 0.5) * 0.44 + 0.24;
      inboundDelays[index] = serviceIndex * 420 + batchIndex * 95 + Math.random() * 120;
      inboundPositions.set([
        Math.cos(angle) * radius,
        Math.sin(angle) * radius * 0.72,
        depth
      ], index * 3);
    }

    const inboundGeometry = new THREE.BufferGeometry();
    inboundGeometry.setAttribute("position", new THREE.BufferAttribute(inboundPositions, 3));
    inboundGeometry.setAttribute("aAlpha", new THREE.BufferAttribute(inboundAlphas, 1));
    const inboundMaterial = new THREE.ShaderMaterial({
      transparent: true,
      depthWrite: false,
      blending: THREE.NormalBlending,
      vertexShader: `
        attribute float aAlpha;
        varying float vAlpha;
        void main() {
          vAlpha = aAlpha;
          vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
          gl_PointSize = (1.0 + aAlpha * 0.8) * (7.0 / max(3.0, -mvPosition.z));
          gl_Position = projectionMatrix * mvPosition;
        }
      `,
      fragmentShader: `
        varying float vAlpha;
        void main() {
          float distanceToCenter = distance(gl_PointCoord, vec2(0.5));
          if (distanceToCenter > 0.5) discard;
          float softEdge = 1.0 - smoothstep(0.34, 0.5, distanceToCenter);
          float solidCore = 1.0 - smoothstep(0.0, 0.3, distanceToCenter);
          vec3 deepSeaBlue = vec3(0.015, 0.18, 0.32);
          vec3 deepTeal = vec3(0.02, 0.48, 0.56);
          vec3 color = mix(deepSeaBlue, deepTeal, 0.25 + vAlpha * 0.65);
          float opacity = vAlpha * (0.78 + solidCore * 0.22) * softEdge;
          gl_FragColor = vec4(color, opacity);
        }
      `
    });
    const inboundParticles = new THREE.Points(inboundGeometry, inboundMaterial);
    inboundParticles.rotation.x = -0.08;
    inboundParticles.visible = !reducedMotion;
    scene.add(inboundParticles);

    let startedAt = 0;
    let controller;
    const render = (time = performance.now()) => {
      if (!reducedMotion) {
        const elapsed = startedAt ? Math.min(1, (time - startedAt) / 4400) : 0;
        const firstPhase = Math.min(1, elapsed / 0.55);
        const secondPhase = Math.max(0, Math.min(1, (elapsed - 0.55) / 0.45));
        const clusterEase = 1 - Math.pow(1 - firstPhase, 4);
        const ringEase = 1 - Math.pow(1 - secondPhase, 4);
        const positionArray = geometry.attributes.position.array;
        for (let index = 0; index < particleCount * 3; index += 1) {
          const clustered = starts[index] + (clusterTargets[index] - starts[index]) * clusterEase;
          positionArray[index] = clustered + (targets[index] - clusterTargets[index]) * ringEase;
        }
        geometry.attributes.position.needsUpdate = true;
        particles.rotation.z = elapsed * 0.1 + Math.max(0, time - startedAt - 4400) * 0.000035;

        const flowElapsed = Math.max(0, time - startedAt - 3800);
        const flowCycle = 7600;
        const inboundPositionArray = inboundGeometry.attributes.position.array;
        const inboundAlphaArray = inboundGeometry.attributes.aAlpha.array;
        for (let index = 0; index < inboundCount; index += 1) {
          const particleElapsed = flowElapsed - inboundDelays[index];
          const offset = index * 3;
          if (particleElapsed < 0) {
            inboundAlphaArray[index] = 0;
            continue;
          }

          const progress = (particleElapsed % flowCycle) / flowCycle;
          const travel = 1 - Math.pow(1 - progress, 2.25);
          const sourceRadius = inboundRadii[index];
          const radius = sourceRadius * (1 - travel) + 0.12 * travel;
          const angle = inboundAngles[index]
            + inboundTwists[index] * travel
            + Math.sin(progress * Math.PI) * 0.1;
          const fadeIn = Math.min(1, progress / 0.12);
          const fadeOut = Math.min(1, (1 - progress) / 0.18);
          const alpha = Math.min(fadeIn, fadeOut) * (0.58 + 0.42 * Math.sin(progress * Math.PI));

          inboundPositionArray[offset] = Math.cos(angle) * radius;
          inboundPositionArray[offset + 1] = Math.sin(angle) * radius * 0.72;
          inboundPositionArray[offset + 2] = inboundDepths[index] * (1 - travel)
            + Math.sin(progress * Math.PI * 2) * 0.05;
          inboundAlphaArray[index] = alpha;
        }
        inboundGeometry.attributes.position.needsUpdate = true;
        inboundGeometry.attributes.aAlpha.needsUpdate = true;
      }
      renderer.render(scene, camera);
    };

    controller = createStageController(stage, render, reducedMotion, () => {
      startedAt = performance.now();
    });
    const resizeObserver = observeRendererSize(THREE, stage, renderer, camera, controller.renderOnce);
    canvas.addEventListener("webglcontextlost", (event) => {
      event.preventDefault();
      controller.dispose();
      resizeObserver.disconnect();
      markFallback(stage);
    }, { once: true });
    stage.classList.add("is-webgl-ready");
    if (reducedMotion) render();
  } catch {
    markFallback(stage);
  }
}

function initAgentFlow(THREE) {
  const canvas = document.querySelector("[data-agent-canvas]");
  const stage = canvas?.closest("[data-webgl-stage]");
  if (!canvas || !stage) return;

  try {
    const renderer = createRenderer(THREE, canvas);
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(34, 1, 0.1, 100);
    camera.position.set(0, 0, 7.5);

    const compact = window.matchMedia("(max-width: 680px)").matches;
    const columnCount = compact ? 28 : 46;
    const rowCount = compact ? 30 : 40;
    const pointCount = columnCount * rowCount;
    const positions = new Float32Array(pointCount * 3);
    const offsets = new Float32Array(pointCount);
    const speeds = new Float32Array(pointCount);
    const bits = new Float32Array(pointCount);
    const brightnesses = new Float32Array(pointCount);
    const heats = new Float32Array(pointCount);
    const phases = new Float32Array(pointCount);
    const columnSpeeds = Array.from({ length: columnCount }, () => 0.18 + Math.random() * 0.2);

    for (let column = 0; column < columnCount; column += 1) {
      for (let row = 0; row < rowCount; row += 1) {
        const index = column * rowCount + row;
        const offset = index * 3;
        const layer = (column + row) % 3;
        const x = -2.55 + (column / Math.max(1, columnCount - 1)) * 5.1
          + (Math.random() - 0.5) * 0.035;

        positions.set([x, 0, -layer * 0.12], offset);
        offsets[index] = row / rowCount + Math.random() * 0.025;
        speeds[index] = columnSpeeds[column];
        bits[index] = Math.random() > 0.5 ? 1 : 0;
        brightnesses[index] = 0.08 + Math.random() * 0.16 + layer * 0.025;
        heats[index] = Math.random() < 0.075 ? 0.72 + Math.random() * 0.28 : Math.random() * 0.08;
        phases[index] = Math.random();
      }
    }

    const glyphCanvas = document.createElement("canvas");
    glyphCanvas.width = 64;
    glyphCanvas.height = 32;
    const glyphContext = glyphCanvas.getContext("2d");
    if (!glyphContext) throw new Error("Binary glyph atlas unavailable");
    glyphContext.clearRect(0, 0, glyphCanvas.width, glyphCanvas.height);
    glyphContext.fillStyle = "#ffffff";
    glyphContext.font = "700 25px Consolas, monospace";
    glyphContext.textAlign = "center";
    glyphContext.textBaseline = "middle";
    glyphContext.fillText("0", 16, 16);
    glyphContext.fillText("1", 48, 16);

    const glyphTexture = new THREE.CanvasTexture(glyphCanvas);
    glyphTexture.minFilter = THREE.LinearFilter;
    glyphTexture.magFilter = THREE.LinearFilter;
    glyphTexture.generateMipmaps = false;
    glyphTexture.needsUpdate = true;

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
    geometry.setAttribute("aOffset", new THREE.BufferAttribute(offsets, 1));
    geometry.setAttribute("aSpeed", new THREE.BufferAttribute(speeds, 1));
    geometry.setAttribute("aBit", new THREE.BufferAttribute(bits, 1));
    geometry.setAttribute("aBrightness", new THREE.BufferAttribute(brightnesses, 1));
    geometry.setAttribute("aHeat", new THREE.BufferAttribute(heats, 1));
    geometry.setAttribute("aPhase", new THREE.BufferAttribute(phases, 1));
    const material = new THREE.ShaderMaterial({
      transparent: true,
      depthWrite: false,
      blending: THREE.NormalBlending,
      uniforms: {
        uTime: { value: 0 },
        uMotion: { value: reducedMotion ? 0 : 1 },
        uGlyphAtlas: { value: glyphTexture },
        uPointSize: { value: compact ? 13 : 15 }
      },
      vertexShader: `
        uniform float uTime;
        uniform float uMotion;
        uniform float uPointSize;
        attribute float aOffset;
        attribute float aSpeed;
        attribute float aBit;
        attribute float aBrightness;
        attribute float aHeat;
        attribute float aPhase;
        varying float vBit;
        varying float vIntensity;
        void main() {
          float stream = fract(aOffset + uTime * aSpeed * uMotion);
          float pulsePhase = fract(stream * 2.6 + aPhase - uTime * 0.42 * uMotion);
          float pulse = smoothstep(0.78, 0.9, pulsePhase)
            * (1.0 - smoothstep(0.9, 1.0, pulsePhase));
          vIntensity = clamp(aBrightness + aHeat * pulse, 0.0, 1.0);
          vBit = mod(aBit + floor(uTime * (1.4 + aSpeed * 2.0) * uMotion + aPhase * 5.0), 2.0);
          vec3 transformed = position;
          transformed.y = mix(2.55, -2.55, stream);
          vec4 mvPosition = modelViewMatrix * vec4(transformed, 1.0);
          gl_PointSize = uPointSize * (0.8 + vIntensity * 0.28);
          gl_Position = projectionMatrix * mvPosition;
        }
      `,
      fragmentShader: `
        uniform sampler2D uGlyphAtlas;
        varying float vBit;
        varying float vIntensity;
        void main() {
          vec2 atlasUv = vec2((gl_PointCoord.x + vBit) * 0.5, gl_PointCoord.y);
          float glyphAlpha = texture2D(uGlyphAtlas, atlasUv).a;
          if (glyphAlpha < 0.04) discard;
          vec3 deepCyan = vec3(0.12, 0.55, 0.7);
          vec3 hotWhite = vec3(0.82, 0.97, 1.0);
          vec3 color = mix(deepCyan, hotWhite, smoothstep(0.58, 1.0, vIntensity));
          float opacity = glyphAlpha * (0.1 + vIntensity * 0.76);
          gl_FragColor = vec4(color, opacity);
        }
      `
    });
    scene.add(new THREE.Points(geometry, material));

    let controller;
    const render = (time = performance.now()) => {
      material.uniforms.uTime.value = reducedMotion ? 0.82 : time * 0.001;
      renderer.render(scene, camera);
    };
    controller = createStageController(stage, render, reducedMotion);
    const resizeObserver = observeRendererSize(THREE, stage, renderer, camera, controller.renderOnce);
    canvas.addEventListener("webglcontextlost", (event) => {
      event.preventDefault();
      controller.dispose();
      resizeObserver.disconnect();
      glyphTexture.dispose();
      markFallback(stage);
    }, { once: true });
    stage.classList.add("is-webgl-ready");
    if (reducedMotion) render();
  } catch {
    markFallback(stage);
  }
}

async function initWebGLTopics() {
  const stages = Array.from(document.querySelectorAll("[data-webgl-stage]"));
  if (!supportsWebGL()) {
    stages.forEach(markFallback);
    return;
  }

  try {
    const THREE = await import("/prototypes/vendor/three.module.min.js");
    initEcosystem(THREE);
    initAgentFlow(THREE);
  } catch {
    stages.forEach(markFallback);
  }
}

void initWebGLTopics();
