package com.gradlelighthouse.task

object LighthouseGraphJs {
    val SCRIPT = """
        // -----------------------------------------------------------------------------
        // Premium Gamified Galaxy Graph Engine (Jitter-free)
        // -----------------------------------------------------------------------------
        const defaultLayerColors = {
            'App': '#FF9800', 'Feature': '#06b6d4', 'Core': '#10b981',
            'Domain': '#8b5cf6', 'Data': '#3b82f6', 'Shared': '#64748b', 'Root': '#ec4899'
        };

        function getLayerColor(layerName) {
            if (defaultLayerColors[layerName]) return defaultLayerColors[layerName];
            // Dynamically generate a vibrant color for unknown layers based on string hash
            let hash = 0;
            for (let i = 0; i < layerName.length; i++) hash = layerName.charCodeAt(i) + ((hash << 5) - hash);
            const hue = Math.abs(hash) % 360;
            return `hsl(${'$'}{hue}, 85%, 65%)`;
        }

        const layerOrder = {
            'Root': 0, 'App': 1, 'Feature': 2, 'Domain': 3, 'Data': 4, 'Core': 5, 'Shared': 6
        };

        function isIllegalDependency(sourceLayer, targetLayer) {
            const srcIdx = layerOrder[sourceLayer] !== undefined ? layerOrder[sourceLayer] : 99;
            const tgtIdx = layerOrder[targetLayer] !== undefined ? layerOrder[targetLayer] : 99;
            // A lower-level layer depending on a higher-level layer is illegal (e.g. Core depending on Feature)
            return srcIdx > tgtIdx;
        }

        let graphInstance;
        let fsGraphInstance;
        let cycleNodes = new Set();
        let cycleEdges = new Set();
        let showCyclesOnly = false;
        let layerViewEnabled = false; // "Orbit View"
        let currentSearchQuery = '';
        let focusedModule = null; // for drill-down
        let perfectModeEnabled = false; // "Perfect simulation mode"
        let activeIsolatedLayer = null;
        let originalLinks = [];
        let cutLinks = [];

        function distToSegment(p, v, w) {
            const l2 = Math.hypot(v.x - w.x, v.y - w.y) ** 2;
            if (l2 === 0) return Math.hypot(p.x - v.x, p.y - v.y);
            let t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2;
            t = Math.max(0, Math.min(1, t));
            return Math.hypot(p.x - (v.x + t * (w.x - v.x)), p.y - (v.y + t * (w.y - v.y)));
        }

        // Gamification / XP
        function calculateXP(nodes) {
            if (perfectModeEnabled) {
                const xp = nodes.length * 1000;
                return { xp, perfectModules: nodes.length, level: Math.floor(xp / 1000) + 1 };
            }
            let xp = 0;
            let perfectModules = 0;
            nodes.forEach(n => {
                xp += n.score * 10;
                if(n.score === 100) perfectModules++;
            });
            return { xp, perfectModules, level: Math.floor(xp / 1000) + 1 };
        }

        function initGraphData() {
            const adj = {};
            if (graphData && graphData.nodes) {
                graphData.nodes.forEach(n => adj[n.id] = []);
                graphData.links.forEach(l => { if (adj[l.source]) adj[l.source].push(l.target); });

                const visited = {}, recStack = {};
                function dfs(curr, path) {
                    visited[curr] = true; recStack[curr] = true; path.push(curr);
                    const neighbors = adj[curr] || [];
                    for (const neighbor of neighbors) {
                        if (neighbor === curr) continue; // Skip self-loops
                        if (recStack[neighbor]) {
                            const cycleStartIdx = path.indexOf(neighbor);
                            const cycle = path.slice(cycleStartIdx);
                            cycle.forEach(node => cycleNodes.add(node));
                            for (let i = 0; i < cycle.length; i++) {
                                cycleEdges.add(cycle[i] + '->' + cycle[(i + 1) % cycle.length]);
                            }
                        } else if (!visited[neighbor]) dfs(neighbor, path);
                    }
                    recStack[curr] = false; path.pop();
                }
                Object.keys(adj).forEach(node => { if (!visited[node]) dfs(node, []); });
            }
        }

        class ModuleGraph {
            constructor(canvas, isFullscreen, minimapCanvas = null) {
                this.canvas = canvas;
                this.ctx = canvas.getContext('2d');
                this.isFullscreen = isFullscreen;
                this.minimapCanvas = minimapCanvas;
                this.minimapCtx = minimapCanvas ? minimapCanvas.getContext('2d') : null;

                this.nodes = [];
                this.links = [];
                this.panX = 0; this.panY = 0; this.zoom = 1;
                this.draggedNode = null; this.hoveredNode = null;
                this.selectedNode = null;
                this.selectedLink = null;
                this.hoveredLink = null;

                // ── HiDPI-safe canvas sizing ──────────────────────────────────────────
                // Rules:
                //   • Always use setTransform() instead of scale() so the DPR matrix is
                //     replaced (not accumulated) every time the canvas is resized.
                //   • Assigning canvas.width/height automatically resets the 2D context
                //     transform to identity — so setTransform must come AFTER the assignment.
                //   • ResizeObserver fires once synchronously right after .observe(), which
                //     is fine because we skip the redundant first call with the flag below.
                const dpr = window.devicePixelRatio || 1;

                const applySize = (w, h) => {
                    this.width  = w || 800;
                    this.height = h || 500;
                    canvas.width  = this.width  * dpr;
                    canvas.height = this.height * dpr;
                    // setTransform replaces the current matrix — never stacks
                    this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
                };

                // Initial sizing — use getBoundingClientRect first, offsetWidth as fallback
                const rect = canvas.parentElement.getBoundingClientRect();
                applySize(
                    rect.width  || canvas.parentElement.offsetWidth,
                    rect.height || canvas.parentElement.offsetHeight
                );

                let _roFirstCall = true;
                const resizeObserver = new ResizeObserver(entries => {
                    // Skip the synchronous first callback that fires immediately on .observe()
                    // to avoid double-initialisation with the same dimensions.
                    if (_roFirstCall) { _roFirstCall = false; return; }
                    for (const entry of entries) {
                        applySize(entry.contentRect.width, entry.contentRect.height);
                        this.render();
                    }
                });
                resizeObserver.observe(canvas.parentElement);

                if (this.minimapCanvas) {
                    const mRect = minimapCanvas.parentElement.getBoundingClientRect();
                    minimapCanvas.width  = (mRect.width  || 160) * dpr;
                    minimapCanvas.height = (mRect.height || 110) * dpr;
                    this.minimapCtx.setTransform(dpr, 0, 0, dpr, 0, 0);
                }

                this.initNodes();
                this.setupEvents();
                this.animate();
            }

            initNodes() {
                // Normalize link endpoints: after a sandbox cut/reset graphData.links may have
                // already-resolved node objects instead of plain string IDs — handle both forms.
                const linkId = v => (v && typeof v === 'object') ? v.id : v;
                this.nodes = graphData.nodes.map(n => {
                    const degree = graphData.links.filter(l => linkId(l.source) === n.id || linkId(l.target) === n.id).length;
                    return {
                        ...n, degree,
                        x: this.width/2 + (Math.random()-0.5)*200,
                        y: this.height/2 + (Math.random()-0.5)*200,
                        vx: 0, vy: 0,
                        radius: Math.max(8 + degree * 1.5, 10 + (n.fatalCount * 3)),
                        isCycle: cycleNodes.has(n.id),
                        color: getLayerColor(n.layer)
                    };
                });
                this.links = graphData.links.map(l => ({
                    source: this.nodes.find(n => n.id === linkId(l.source)),
                    target: this.nodes.find(n => n.id === linkId(l.target)),
                    isCycle: cycleEdges.has(linkId(l.source) + '->' + linkId(l.target))
                })).filter(l => l.source && l.target && l.source.id !== l.target.id);
            }

            setupEvents() {
                const getPos = (e) => {
                    const rect = this.canvas.getBoundingClientRect();
                    const x = (e.clientX - rect.left - this.width/2 - this.panX)/this.zoom + this.width/2;
                    const y = (e.clientY - rect.top - this.height/2 - this.panY)/this.zoom + this.height/2;
                    return {x, y, cx: e.clientX, cy: e.clientY};
                };

                // Touch helper — maps a Touch point to the same world-space coordinates as getPos()
                const getTouchPos = (touch) => {
                    const rect = this.canvas.getBoundingClientRect();
                    const x = (touch.clientX - rect.left - this.width/2 - this.panX)/this.zoom + this.width/2;
                    const y = (touch.clientY - rect.top - this.height/2 - this.panY)/this.zoom + this.height/2;
                    return {x, y};
                };

                // ── Mouse events ──────────────────────────────────────────────────────────
                this.canvas.addEventListener('mousedown', e => {
                    const pos = getPos(e);
                    const clicked = this.nodes.find(n => Math.hypot(n.x - pos.x, n.y - pos.y) <= n.radius + 5);
                    if (clicked) {
                        this.draggedNode = clicked;
                        this.selectedNode = clicked;
                        this.selectedLink = null;
                        updateNodeDetails(clicked, this.isFullscreen);
                    } else {
                        const clickedL = this.links.find(l => distToSegment(pos, l.source, l.target) < 6);
                        if (clickedL) {
                            this.selectedLink = clickedL;
                            this.selectedNode = null;
                            this.draggedNode = null;
                            updateLinkDetails(clickedL, this.isFullscreen);
                        } else {
                            this.draggedNode = { isPan: true, startX: e.clientX - this.panX, startY: e.clientY - this.panY };
                            this.selectedNode = null;
                            this.selectedLink = null;
                            updateNodeDetails(null, this.isFullscreen);
                        }
                    }
                });

                this.canvas.addEventListener('mousemove', e => {
                    const pos = getPos(e);

                    // Node hover takes priority
                    const hoveredN = this.nodes.find(n => Math.hypot(n.x - pos.x, n.y - pos.y) <= n.radius + 5);
                    if (hoveredN) {
                        this.hoveredNode = hoveredN;
                        this.hoveredLink = null;
                        this.canvas.style.cursor = 'pointer';
                    } else if (!this.draggedNode) {
                        // Check link hover
                        const hoveredL = this.links.find(l => distToSegment(pos, l.source, l.target) < 6);
                        if (hoveredL) {
                            this.hoveredLink = hoveredL;
                            this.hoveredNode = null;
                            this.canvas.style.cursor = 'pointer';
                        } else {
                            this.hoveredNode = null;
                            this.hoveredLink = null;
                            this.canvas.style.cursor = 'grab';
                        }
                    } else {
                        this.canvas.style.cursor = 'grabbing';
                    }

                    if (this.draggedNode) {
                        if (this.draggedNode.isPan) {
                            this.panX = e.clientX - this.draggedNode.startX;
                            this.panY = e.clientY - this.draggedNode.startY;
                        } else {
                            this.draggedNode.x = pos.x; this.draggedNode.y = pos.y;
                            this.draggedNode.vx = 0; this.draggedNode.vy = 0;
                        }
                    }
                });

                this.canvas.addEventListener('mouseup', () => this.draggedNode = null);
                this.canvas.addEventListener('mouseleave', () => { this.draggedNode = null; this.hoveredNode = null; this.hoveredLink = null; });
                this.canvas.addEventListener('dblclick', e => {
                    const pos = getPos(e);
                    const clicked = this.nodes.find(n => Math.hypot(n.x - pos.x, n.y - pos.y) <= n.radius + 5);
                    if (clicked) {
                        focusedModule = focusedModule === clicked.id ? null : clicked.id;
                        if(fsGraphInstance) fsGraphInstance.resetPhysics();
                        if(graphInstance) graphInstance.resetPhysics();
                        updateBreadcrumb();
                    }
                });
                this.canvas.addEventListener('wheel', e => {
                    e.preventDefault();
                    this.zoom = Math.max(0.1, Math.min(5, this.zoom * (e.deltaY < 0 ? 1.1 : 0.9)));
                });

                // ── Touch events (mobile & tablet support) ────────────────────────────────
                // Tracks state for single-finger drag/tap and two-finger pinch-zoom.
                let lastTouchDist = null;  // distance between two fingers on previous frame
                let tapTimeout    = null;  // for distinguishing tap vs. double-tap

                this.canvas.addEventListener('touchstart', e => {
                    e.preventDefault();
                    if (e.touches.length === 1) {
                        const pos = getTouchPos(e.touches[0]);
                        const clicked = this.nodes.find(n => Math.hypot(n.x - pos.x, n.y - pos.y) <= n.radius + 8);
                        if (clicked) {
                            this.draggedNode = clicked;
                            this.selectedNode = clicked;
                            this.selectedLink = null;
                            updateNodeDetails(clicked, this.isFullscreen);

                            // Double-tap detection: focus/unfocus a module
                            if (tapTimeout) {
                                clearTimeout(tapTimeout);
                                tapTimeout = null;
                                focusedModule = focusedModule === clicked.id ? null : clicked.id;
                                if (fsGraphInstance) fsGraphInstance.resetPhysics();
                                if (graphInstance)   graphInstance.resetPhysics();
                                updateBreadcrumb();
                            } else {
                                tapTimeout = setTimeout(() => { tapTimeout = null; }, 300);
                            }
                        } else {
                            this.draggedNode = { isPan: true, startX: e.touches[0].clientX - this.panX, startY: e.touches[0].clientY - this.panY };
                            this.selectedNode = null;
                            this.selectedLink = null;
                            updateNodeDetails(null, this.isFullscreen);
                        }
                        lastTouchDist = null;
                    } else if (e.touches.length === 2) {
                        // Start of a pinch gesture — stop any ongoing drag
                        this.draggedNode = null;
                        const dx = e.touches[0].clientX - e.touches[1].clientX;
                        const dy = e.touches[0].clientY - e.touches[1].clientY;
                        lastTouchDist = Math.hypot(dx, dy);
                    }
                }, { passive: false });

                this.canvas.addEventListener('touchmove', e => {
                    e.preventDefault();
                    if (e.touches.length === 1 && this.draggedNode) {
                        if (this.draggedNode.isPan) {
                            this.panX = e.touches[0].clientX - this.draggedNode.startX;
                            this.panY = e.touches[0].clientY - this.draggedNode.startY;
                        } else {
                            const pos = getTouchPos(e.touches[0]);
                            this.draggedNode.x = pos.x;
                            this.draggedNode.y = pos.y;
                            this.draggedNode.vx = 0;
                            this.draggedNode.vy = 0;
                        }
                    } else if (e.touches.length === 2 && lastTouchDist !== null) {
                        // Pinch-to-zoom
                        const dx   = e.touches[0].clientX - e.touches[1].clientX;
                        const dy   = e.touches[0].clientY - e.touches[1].clientY;
                        const dist = Math.hypot(dx, dy);
                        const scale = dist / lastTouchDist;
                        this.zoom = Math.max(0.1, Math.min(5, this.zoom * scale));
                        lastTouchDist = dist;
                    }
                }, { passive: false });

                this.canvas.addEventListener('touchend', e => {
                    e.preventDefault();
                    if (e.touches.length === 0) {
                        this.draggedNode  = null;
                        this.hoveredNode  = null;
                        this.hoveredLink  = null;
                        lastTouchDist     = null;
                    } else if (e.touches.length === 1) {
                        // One finger lifted from a pinch — resume single-finger pan
                        lastTouchDist = null;
                        this.draggedNode = { isPan: true, startX: e.touches[0].clientX - this.panX, startY: e.touches[0].clientY - this.panY };
                    }
                }, { passive: false });
            }

            resetPhysics() {
                this.nodes.forEach(n => { n.vx = 0; n.vy = 0; });
            }

            updatePhysics() {
                const kRepulsion = layerViewEnabled ? 200 : 500;
                const kAttraction = 0.02;
                const damping = 0.82;

                const layers = [...new Set(this.nodes.map(n => n.layer))].sort();

                const activeNodes = focusedModule ? this.nodes.filter(n => {
                    if (n.id === focusedModule) return true;
                    return this.links.some(l => (l.source.id === focusedModule && l.target.id === n.id) || (l.target.id === focusedModule && l.source.id === n.id));
                }) : this.nodes;

                for (let i = 0; i < activeNodes.length; i++) {
                    const n1 = activeNodes[i];
                    for (let j = i + 1; j < activeNodes.length; j++) {
                        const n2 = activeNodes[j];
                        let dx = n2.x - n1.x, dy = n2.y - n1.y;
                        if (dx === 0 && dy === 0) { dx = (Math.random()-0.5)*2; dy = (Math.random()-0.5)*2; }
                        const dist = Math.sqrt(dx*dx + dy*dy);

                        const minDistance = n1.radius + n2.radius + 15;
                        if (dist < minDistance) {
                            const overlap = minDistance - dist;
                            const force = overlap * 0.15;
                            const adjustX = (dx / dist) * force;
                            const adjustY = (dy / dist) * force;

                            if (n1 !== this.draggedNode) { n1.vx -= adjustX; n1.vy -= adjustY; }
                            if (n2 !== this.draggedNode) { n2.vx += adjustX; n2.vy += adjustY; }
                        }

                        if (dist < 400) {
                            const f = (kRepulsion * (1 + (n1.radius + n2.radius) / 30)) / (dist * dist);
                            if (n1 !== this.draggedNode) { n1.vx -= (dx/dist)*f; n1.vy -= (dy/dist)*f; }
                            if (n2 !== this.draggedNode) { n2.vx += (dx/dist)*f; n2.vy += (dy/dist)*f; }
                        }
                    }
                }

                this.links.forEach(l => {
                    if (focusedModule && l.source.id !== focusedModule && l.target.id !== focusedModule) return;
                    let dx = l.target.x - l.source.x, dy = l.target.y - l.source.y;
                    if (dx === 0 && dy === 0) { dx = 0.1; dy = 0.1; }
                    const dist = Math.sqrt(dx*dx + dy*dy);

                    const targetDist = 70 + l.source.radius + l.target.radius;
                    const f = kAttraction * (dist - targetDist);
                    if (l.source !== this.draggedNode) { l.source.vx += (dx/dist)*f; l.source.vy += (dy/dist)*f; }
                    if (l.target !== this.draggedNode) { l.target.vx -= (dx/dist)*f; l.target.vy -= (dy/dist)*f; }
                });

                activeNodes.forEach(n => {
                    if (n === this.draggedNode) return;

                    if (layerViewEnabled) {
                        // Solar System Orbits!
                        const layerIndex = layers.indexOf(n.layer);
                        const orbitRadius = layerIndex * 120; // 120px per orbit
                        const cx = this.width/2, cy = this.height/2;

                        if (orbitRadius === 0) {
                            n.vx += (cx - n.x) * 0.05;
                            n.vy += (cy - n.y) * 0.05;
                        } else {
                            const dx = n.x - cx, dy = n.y - cy;
                            const distFromCenter = Math.sqrt(dx*dx + dy*dy) || 1;
                            const f = (orbitRadius - distFromCenter) * 0.02;
                            n.vx += (dx/distFromCenter) * f;
                            n.vy += (dy/distFromCenter) * f;

                            // Slight orbital angular velocity (spin)
                            n.vx -= (dy/distFromCenter) * 0.2;
                            n.vy += (dx/distFromCenter) * 0.2;
                        }
                    } else {
                        // Very soft central gravity
                        n.vx += (this.width/2 - n.x) * 0.002;
                        n.vy += (this.height/2 - n.y) * 0.002;
                    }

                    n.vx = Math.max(-15, Math.min(15, n.vx));
                    n.vy = Math.max(-15, Math.min(15, n.vy));

                    if (Math.abs(n.vx) < 0.03) n.vx = 0;
                    if (Math.abs(n.vy) < 0.03) n.vy = 0;

                    n.x += n.vx; n.y += n.vy;
                    n.vx *= damping; n.vy *= damping;
                });
            }

            render() {
                // Space Background
                this.ctx.fillStyle = '#0a0a0f';
                this.ctx.fillRect(0, 0, this.width, this.height);

                this.ctx.save();
                this.ctx.translate(this.width/2 + this.panX, this.height/2 + this.panY);
                this.ctx.scale(this.zoom, this.zoom);
                this.ctx.translate(-this.width/2, -this.height/2);

                if (layerViewEnabled) {
                    // Draw Orbits
                    const layers = [...new Set(this.nodes.map(n => n.layer))].sort();
                    const cx = this.width/2, cy = this.height/2;
                    this.ctx.lineWidth = 1;
                    layers.forEach((l, i) => {
                        const r = i * 120;
                        if (r > 0) {
                            this.ctx.beginPath();
                            this.ctx.arc(cx, cy, r, 0, Math.PI*2);
                            this.ctx.strokeStyle = `rgba(255,255,255,0.05)`;
                            this.ctx.stroke();
                        }
                        // Draw orbit label
                        this.ctx.fillStyle = 'rgba(255,255,255,0.4)';
                        this.ctx.font = '10px sans-serif';
                        this.ctx.textAlign = 'center';
                        this.ctx.fillText(l, cx, cy - r - 5);
                    });
                }

                // Draw Edges (Energy Links)
                this.ctx.globalCompositeOperation = 'screen';
                this.links.forEach(l => {
                    if (perfectModeEnabled && (l.isCycle || isIllegalDependency(l.source.layer, l.target.layer))) return; // Hide cycles and illegal leaks
                    if (showCyclesOnly && !l.isCycle) return;
                    if (focusedModule && l.source.id !== focusedModule && l.target.id !== focusedModule) return;

                    const isFocus = (this.hoveredNode && (l.source === this.hoveredNode || l.target === this.hoveredNode)) ||
                                    (this.selectedNode && (l.source === this.selectedNode || l.target === this.selectedNode)) ||
                                    (this.hoveredLink === l) || (this.selectedLink === l);

                    this.ctx.beginPath();
                    this.ctx.moveTo(l.source.x, l.source.y);

                    // Visual Edge Bundling: Curve lines towards the center of gravity
                    const cx = this.width / 2;
                    const cy = this.height / 2;
                    const midX = (l.source.x + l.target.x) / 2;
                    const midY = (l.source.y + l.target.y) / 2;
                    const cpX = midX + (cx - midX) * 0.25;
                    const cpY = midY + (cy - midY) * 0.25;

                    this.ctx.quadraticCurveTo(cpX, cpY, l.target.x, l.target.y);

                    if (activeIsolatedLayer && l.source.layer !== activeIsolatedLayer && l.target.layer !== activeIsolatedLayer) {
                        this.ctx.globalAlpha = 0.04; // Extremely dimmed link for isolated layer view
                    } else {
                        this.ctx.globalAlpha = 1.0;
                    }

                    if (l.isCycle) {
                        this.ctx.strokeStyle = '#ef4444';
                        this.ctx.lineWidth = isFocus ? 3 : 1.5;
                        this.ctx.shadowBlur = 10;
                        this.ctx.shadowColor = '#ef4444';
                    } else if (this.selectedLink === l) {
                        this.ctx.strokeStyle = '#f59e0b';
                        this.ctx.lineWidth = 3.5;
                        this.ctx.shadowBlur = 12;
                        this.ctx.shadowColor = '#f59e0b';
                    } else if (this.hoveredLink === l) {
                        this.ctx.strokeStyle = '#FF9800';
                        this.ctx.lineWidth = 2.5;
                        this.ctx.shadowBlur = 10;
                        this.ctx.shadowColor = '#FF9800';
                    } else if (isFocus) {
                        this.ctx.strokeStyle = '#FF9800';
                        this.ctx.lineWidth = 2;
                        this.ctx.shadowBlur = 8;
                        this.ctx.shadowColor = '#FF9800';
                    } else {
                        this.ctx.strokeStyle = 'rgba(100,116,139,0.3)';
                        this.ctx.lineWidth = 1;
                        this.ctx.shadowBlur = 0;
                    }
                    this.ctx.stroke();
                });
                this.ctx.globalAlpha = 1.0;

                // Draw Nodes (Planets)
                this.ctx.globalCompositeOperation = 'source-over';
                this.nodes.forEach(n => {
                    if (showCyclesOnly && !n.isCycle && !perfectModeEnabled) return;
                    if (focusedModule && n.id !== focusedModule && !this.links.some(l => (l.source.id === focusedModule && l.target.id === n.id) || (l.target.id === focusedModule && l.source.id === n.id))) return;

                    const isHovered = this.hoveredNode === n || this.selectedNode === n;
                    const matchesSearch = currentSearchQuery && n.id.toLowerCase().includes(currentSearchQuery.toLowerCase());
                    const matchesLayer = !activeIsolatedLayer || n.layer === activeIsolatedLayer;
                    const color = n.color;

                    this.ctx.globalAlpha = (activeIsolatedLayer && !matchesLayer) ? 0.15 : (currentSearchQuery && !matchesSearch) ? 0.2 : 1.0;

                    // Planet glow
                    this.ctx.shadowBlur = isHovered ? 25 : 15;
                    this.ctx.shadowColor = color;

                    // Core gradient
                    const grad = this.ctx.createRadialGradient(n.x - n.radius/3, n.y - n.radius/3, n.radius/5, n.x, n.y, n.radius);
                    grad.addColorStop(0, '#ffffff');
                    grad.addColorStop(0.3, color);
                    grad.addColorStop(1, '#000000');

                    if (n.isCycle && !perfectModeEnabled) {
                        this.ctx.beginPath(); this.ctx.arc(n.x, n.y, n.radius + 6, 0, Math.PI*2);
                        this.ctx.fillStyle = 'rgba(239,68,68,0.3)'; this.ctx.fill();
                    } else if (perfectModeEnabled) {
                        // Draw perfect emerald orbit halo around the original layer color planet
                        this.ctx.beginPath(); this.ctx.arc(n.x, n.y, n.radius + 5, 0, Math.PI*2);
                        this.ctx.strokeStyle = '#10b981';
                        this.ctx.lineWidth = 1.5;
                        this.ctx.stroke();
                    }

                    this.ctx.beginPath(); this.ctx.arc(n.x, n.y, n.radius, 0, Math.PI*2);
                    this.ctx.fillStyle = grad; this.ctx.fill();

                    // Reset shadow for text
                    this.ctx.shadowBlur = 0;
                    this.ctx.fillStyle = 'rgba(255,255,255,0.85)';
                    this.ctx.font = isHovered ? 'bold 12px sans-serif' : '10px sans-serif';
                    this.ctx.textAlign = 'center';
                    if (isHovered || matchesSearch || this.zoom > 1.5) {
                        this.ctx.fillText(n.name, n.x, n.y + n.radius + 12);
                    }
                    this.ctx.globalAlpha = 1.0;
                });

                this.ctx.restore();

                if (this.minimapCtx) {
                    this.minimapCtx.fillStyle = '#0a0a0f';
                    this.minimapCtx.fillRect(0,0,160,110);
                    this.minimapCtx.save();
                    const scaleX = 160/this.width;
                    const scaleY = 110/this.height;
                    this.minimapCtx.scale(scaleX, scaleY);
                    this.nodes.forEach(n => {
                        this.minimapCtx.fillStyle = n.color;
                        this.minimapCtx.fillRect(n.x, n.y, 10, 10);
                    });
                    // Mirror the actual render transform:
                    //   translate(width/2 + panX, height/2 + panY) scale(zoom) translate(-width/2, -height/2)
                    // Solving for the top-left world coordinate of the visible viewport:
                    //   worldLeft = width/2  * (1 - 1/zoom) - panX/zoom
                    //   worldTop  = height/2 * (1 - 1/zoom) - panY/zoom
                    const worldLeft = this.width/2  * (1 - 1/this.zoom) - this.panX/this.zoom;
                    const worldTop  = this.height/2 * (1 - 1/this.zoom) - this.panY/this.zoom;
                    this.minimapCtx.strokeStyle = 'rgba(255,255,255,0.5)';
                    this.minimapCtx.lineWidth = 1 / Math.min(scaleX, scaleY);
                    this.minimapCtx.strokeRect(
                        worldLeft, worldTop,
                        this.width/this.zoom, this.height/this.zoom
                    );
                    this.minimapCtx.restore();
                }
            }

            animate() {
                this.updatePhysics();
                this.render();
                requestAnimationFrame(() => this.animate());
            }
        }

        function findCyclePath(startId) {
            const adj = {};
            graphData.links.forEach(l => {
                if (!adj[l.source]) adj[l.source] = [];
                adj[l.source].push(l.target);
            });

            const queue = [[startId]];
            const visited = new Set();

            while (queue.length > 0) {
                const path = queue.shift();
                const curr = path[path.length - 1];
                const neighbors = adj[curr] || [];

                for (const neighbor of neighbors) {
                    if (neighbor === curr) continue; // Skip self-loops
                    if (neighbor === startId && path.length > 1) {
                        return [...path, startId]; // Found cycle back to start!
                    }
                    const pathKey = path.join('->') + '->' + neighbor;
                    if (!visited.has(pathKey)) {
                        visited.add(pathKey);
                        if (path.length < 15) { // Protect recursion depth
                            queue.push([...path, neighbor]);
                        }
                    }
                }
            }
            return null;
        }

        function updateNodeDetails(node, isFS) {
            const panel = document.getElementById(isFS ? 'fs-node-detail' : 'graph-node-details');
            if (!node) {
                panel.innerHTML = '<div style="text-align:center; padding:40px 0; color:var(--text-muted);"><div style="font-size:3rem; color: #334155;">🌌</div><div style="color: rgba(255,255,255,0.5); margin-top:10px;">Select a planetary module</div></div>';
                return;
            }
            const color = node.color;
            // graphData.links always stores raw { source: string, target: string } objects.
            // Normalize both sides to their string id so the comparison is always string === string,
            // regardless of whether a link object has been partially mutated.
            const linkSrc = l => (typeof l.source === 'object' && l.source !== null) ? l.source.id : l.source;
            const linkTgt = l => (typeof l.target === 'object' && l.target !== null) ? l.target.id : l.target;
            const inDeps  = graphData.links.filter(l => linkTgt(l) === node.id).map(l => linkSrc(l));
            const outDeps = graphData.links.filter(l => linkSrc(l) === node.id).map(l => linkTgt(l));

            panel.innerHTML = `
                <div style="border-left: 4px solid ${"$"}{color}; padding-left:12px; margin-bottom:20px;">
                    <h3 style="margin:0; font-size:1.1rem; color:white;">${"$"}{node.id}</h3>
                    <span style="font-size:0.7rem; background:${"$"}{color}33; color:${"$"}{color}; padding:2px 6px; border-radius:4px; font-weight:bold;">${"$"}{node.layer} Layer</span>
                </div>
                <div style="display:flex; gap:10px; margin-bottom:20px;">
                    <div style="background:rgba(255,255,255,0.05); padding:10px; flex:1; text-align:center; border-radius:8px; border: 1px solid rgba(255,255,255,0.1);">
                        <div style="font-size:1.4rem; font-weight:bold; color:${"$"}{node.score>=90?'#10b981':node.score>=70?'#f59e0b':'#dc2626'}">${"$"}{node.score}%</div>
                        <div style="font-size:0.6rem; text-transform:uppercase; color:rgba(255,255,255,0.4);">Health</div>
                    </div>
                    <div style="background:rgba(255,255,255,0.05); padding:10px; flex:1; text-align:center; border-radius:8px; border: 1px solid rgba(255,255,255,0.1);">
                        <div style="font-size:1.4rem; font-weight:bold; color:white;">${"$"}{node.degree}</div>
                        <div style="font-size:0.6rem; text-transform:uppercase; color:rgba(255,255,255,0.4);">Coupling</div>
                    </div>
                </div>
                ${"$"}{node.isCycle ? (() => {
                    const cyclePath = findCyclePath(node.id) || [node.id];
                    const leakSource = cyclePath.length > 2 ? cyclePath[cyclePath.length - 2] : 'Target';
                    return `
                    <div style="background:rgba(239,68,68,0.15); border: 1px solid rgba(239,68,68,0.3); padding:12px; border-radius:8px; margin-bottom:15px;">
                        <div style="color:#ef4444; font-weight:bold; font-size:0.8rem; margin-bottom:8px; display:flex; align-items:center; gap:6px;">
                            <span>⚠️</span> Dynamic Cycle Path Traced
                        </div>

                        <div style="background:rgba(239,68,68,0.15); border: 1px dashed rgba(239,68,68,0.4); border-radius:6px; padding:8px; margin-bottom:10px; font-family:'SF Mono', monospace; font-size:0.65rem; color:#f87171; overflow-x:auto; white-space:nowrap; display:flex; align-items:center; gap:4px;">
                            ${"$"}{cyclePath.map(id => `<span style="background:rgba(239,68,68,0.3); padding:2px 4px; border-radius:4px; font-weight:bold; border:1px solid rgba(239,68,68,0.3);">${"$"}{id}</span>`).join('<span style="color:rgba(255,255,255,0.4); font-weight:bold;">➔</span>')}
                        </div>

                        <div style="font-size:0.7rem; color:rgba(255,255,255,0.7); margin-bottom:10px; line-height:1.45;">
                            Module loop detected! To fix this, you must decouple the backward leak where <code style="color:#f87171; font-weight:bold;">${"$"}{leakSource}</code> depends on <code style="color:#f87171; font-weight:bold;">${"$"}{node.id}</code>.
                        </div>
                        <div style="font-size:0.72rem; color:#f87171; font-weight:bold; margin-bottom:6px; text-transform:uppercase; letter-spacing:0.5px;">🛠️ Refactoring Playbook:</div>
                        <ul style="margin:0; padding-left:14px; font-size:0.7rem; color:rgba(255,255,255,0.85); display:flex; flex-direction:column; gap:6px; line-height:1.35;">
                            <li><strong>1. Dependency Inversion:</strong> Create an interface in <code style="color:#a7f3d0;">${"$"}{node.id}</code> and let <code style="color:#f87171;">${"$"}{leakSource}</code> depend on it dynamically via Hilt/Dagger DI.</li>
                            <li><strong>2. Extract Common:</strong> Move shared elements causing the loop into a separate, clean shared module (like <code>:core:model</code>).</li>
                            <li><strong>3. Module Merger:</strong> If <code style="color:#a7f3d0;">${"$"}{node.id}</code> and <code style="color:#f87171;">${"$"}{leakSource}</code> are highly coupled, merge them into one single module to break the loop.</li>
                        </ul>
                    </div>
                    `;
                })() : ''}
                <div style="margin-bottom:15px;">
                    <div style="font-size:0.75rem; color:rgba(255,255,255,0.4); margin-bottom:4px;">Depends On (${"$"}{outDeps.length})</div>
                    <div style="display:flex; flex-wrap:wrap; gap:4px;">${"$"}{outDeps.map(d=>`<span style="background:rgba(255,255,255,0.08); color:rgba(255,255,255,0.8); font-size:0.7rem; padding:2px 6px; border-radius:4px;">${"$"}{d}</span>`).join('') || '-'}</div>
                </div>
                <div>
                    <div style="font-size:0.75rem; color:rgba(255,255,255,0.4); margin-bottom:4px;">Used By (${"$"}{inDeps.length})</div>
                    <div style="display:flex; flex-wrap:wrap; gap:4px;">${"$"}{inDeps.map(d=>`<span style="background:rgba(255,255,255,0.08); color:rgba(255,255,255,0.8); font-size:0.7rem; padding:2px 6px; border-radius:4px;">${"$"}{d}</span>`).join('') || '-'}</div>
                </div>
            `;
        }

        function updateBreadcrumb() {
            const bc = document.getElementById('fs-breadcrumb');
            if (bc) bc.innerHTML = focusedModule ? `Galaxy View &gt; <span style="color:white; font-weight:bold;">${"$"}{focusedModule}</span>` : 'Galaxy View';
        }

        function toggleCyclesFilter() {
            showCyclesOnly = !showCyclesOnly;
            const btn = document.getElementById('btn-cycles');
            if (btn) {
                btn.style.background = showCyclesOnly ? 'rgba(239,68,68,0.2)' : 'rgba(255,255,255,0.06)';
                btn.style.borderColor = showCyclesOnly ? '#ef4444' : 'var(--border)';
                btn.style.color = showCyclesOnly ? '#ef4444' : 'var(--text-dim)';
                btn.style.boxShadow = showCyclesOnly ? '0 0 12px rgba(239,68,68,0.35)' : 'none';
            }
            const cb = document.getElementById('toggle-cycles');
            if (cb) cb.checked = showCyclesOnly;
        }

        function toggleLayerView() {
            layerViewEnabled = !layerViewEnabled;
            document.getElementById('btn-layer-view').style.background = layerViewEnabled ? 'rgba(255,152,0,0.3)' : 'rgba(255,152,0,0.15)';
        }

        function graphSearchFilter(val) { currentSearchQuery = val; }
        function inlineSearchFilter(val) { currentSearchQuery = val; }

        function updateLinkDetails(link, isFullscreen) {
            const panel = document.getElementById(isFullscreen ? 'fs-node-detail' : 'graph-node-details');
            if (!panel) return;

            panel.innerHTML = `
                <div style="border-left: 4px solid #f59e0b; padding-left:12px; margin-bottom:20px;">
                    <h3 style="margin:0; font-size:1.1rem; color:white;">Energy Link</h3>
                    <span style="font-size:0.7rem; background:rgba(245,158,11,0.15); color:#f59e0b; padding:2px 6px; border-radius:4px; font-weight:bold;">Dependency Link</span>
                </div>

                <div style="background:rgba(255,255,255,0.03); border:1px solid rgba(255,255,255,0.08); padding:15px; border-radius:12px; margin-bottom:20px; line-height:1.5;">
                    <div style="font-size:0.75rem; color:rgba(255,255,255,0.4); margin-bottom:6px;">Dependency Connection:</div>
                    <div style="font-size:0.8rem; font-family:monospace; color:#f87171; word-break:break-all; display:flex; flex-direction:column; gap:4px;">
                        <span style="color:#a7f3d0; font-weight:bold;">Source:</span> ${"$"}{link.source.id}
                        <span style="color:rgba(255,255,255,0.4);">⬇ depends on</span>
                        <span style="color:#f87171; font-weight:bold;">Target:</span> ${"$"}{link.target.id}
                    </div>

                    ${"$"}{link.isCycle ? `
                    <div style="margin-top:15px; padding:10px; background:rgba(239,68,68,0.1); border:1px solid rgba(239,68,68,0.2); border-radius:8px; font-size:0.7rem; color:#f87171; display:flex; gap:6px; align-items:flex-start;">
                        <span>⚠️</span> This link forms a circular dependency loop! Cutting it will immediately break the cycle.
                    </div>
                    ` : ''}

                    <button onclick="simulateCutLink('${"$"}{link.source.id}', '${"$"}{link.target.id}')" style="width:100%; margin-top:15px; background:#ef4444; color:white; border:none; padding:10px; border-radius:8px; font-weight:bold; cursor:pointer; display:flex; align-items:center; justify-content:center; gap:6px; box-shadow:0 0 15px rgba(239,68,68,0.3); transition:transform 0.2s; font-size:0.8rem;">
                        ✂️ Cut Dependency Link
                    </button>
                </div>
            `;
        }

        function simulateCutLink(sourceId, targetId) {
            cutLinks.push({ source: sourceId, target: targetId });
            graphData.links = graphData.links.filter(l => !(l.source === sourceId && l.target === targetId));

            cycleNodes.clear();
            cycleEdges.clear();
            initGraphData();

            if (graphInstance) graphInstance.initNodes();
            if (fsGraphInstance) fsGraphInstance.initNodes();

            if (graphInstance) { graphInstance.selectedNode = null; graphInstance.selectedLink = null; }
            if (fsGraphInstance) { fsGraphInstance.selectedNode = null; fsGraphInstance.selectedLink = null; }

            updateNodeDetails(null, false);
            updateNodeDetails(null, true);
            updateXPPanel();
            updateSandboxStatus();
        }

        function resetSandbox() {
            graphData.links = [...originalLinks];
            cutLinks = [];

            cycleNodes.clear();
            cycleEdges.clear();
            initGraphData();

            if (graphInstance) graphInstance.initNodes();
            if (fsGraphInstance) fsGraphInstance.initNodes();

            if (graphInstance) { graphInstance.selectedNode = null; graphInstance.selectedLink = null; }
            if (fsGraphInstance) { fsGraphInstance.selectedNode = null; fsGraphInstance.selectedLink = null; }

            updateNodeDetails(null, false);
            updateNodeDetails(null, true);
            updateXPPanel();
            updateSandboxStatus();
        }

        function updateSandboxStatus() {
            const btn = document.getElementById('btn-reset-sandbox');
            const fsBtn = document.getElementById('fs-btn-reset-sandbox');
            const statusText = cutLinks.length > 0 ? ` ✂️ ${"$"}{cutLinks.length} Cut` : '';

            if (btn) {
                btn.style.display = cutLinks.length > 0 ? 'inline-block' : 'none';
                btn.innerHTML = `🔄 Reset Sandbox ${"$"}{statusText}`;
            }
            if (fsBtn) {
                fsBtn.style.display = cutLinks.length > 0 ? 'inline-block' : 'none';
                fsBtn.innerHTML = `🔄 Reset Sandbox ${"$"}{statusText}`;
            }
        }

        function graphZoomFit() {
            if(fsGraphInstance) { fsGraphInstance.zoom = 1; fsGraphInstance.panX=0; fsGraphInstance.panY=0; }
            if(graphInstance) { graphInstance.zoom = 1; graphInstance.panX=0; graphInstance.panY=0; }
        }
        function fsZoomIn() { if(fsGraphInstance) fsGraphInstance.zoom *= 1.2; }
        function fsZoomOut() { if(fsGraphInstance) fsGraphInstance.zoom /= 1.2; }

        function resetInlineGraph() {
            if(graphInstance) {
                graphInstance.nodes.forEach(n => {
                    n.x = graphInstance.width/2 + (Math.random()-0.5)*200;
                    n.y = graphInstance.height/2 + (Math.random()-0.5)*200;
                });
                graphZoomFit();
            }
        }

        function openFullscreenGraph() {
            document.getElementById('graph-fullscreen-overlay').style.display = 'flex';
            if(!fsGraphInstance) {
                const canvas = document.getElementById('fs-graph-canvas');
                const mini = document.getElementById('fs-minimap-canvas');
                fsGraphInstance = new ModuleGraph(canvas, true, mini);
            }
        }
        function closeFullscreenGraph() {
            document.getElementById('graph-fullscreen-overlay').style.display = 'none';
        }

        function exportGalaxySnapshot() {
            if (!fsGraphInstance) return;

            const canvas = document.getElementById('fs-graph-canvas');
            const ctx = fsGraphInstance.ctx;
            const w = canvas.width;
            const h = canvas.height;

            ctx.save();
            ctx.setTransform(1, 0, 0, 1, 0, 0); // Reset transform for absolute positioning

            // Draw watermark background
            ctx.fillStyle = 'rgba(10, 10, 15, 0.9)';
            ctx.shadowColor = '#000';
            ctx.shadowBlur = 15;
            ctx.fillRect(w - 360, h - 90, 340, 70);

            // Draw logo/title
            ctx.fillStyle = '#FF9800';
            ctx.font = 'bold 18px sans-serif';
            ctx.shadowBlur = 0;
            ctx.fillText('🌌 Gradle Lighthouse', w - 340, h - 60);

            // Get XP stats
            let xpStats = { level: 1, xp: 0 };
            if (graphData && graphData.nodes) {
                xpStats = calculateXP(graphData.nodes);
            }

            // Draw stats
            ctx.fillStyle = '#94a3b8';
            ctx.font = '14px sans-serif';
            ctx.fillText(`Architect Level ${'$'}{xpStats.level} | ${'$'}{xpStats.xp} XP`, w - 340, h - 35);

            // Draw Legend
            const layers = [...new Set(graphData.nodes.map(n => n.layer))].sort();
            ctx.fillStyle = 'rgba(10, 10, 15, 0.9)';
            // Increase height of box to accommodate custom lines
            ctx.fillRect(20, h - 30 - (layers.length * 25) - 40, 180, (layers.length * 25) + 50);

            ctx.textAlign = 'left';
            ctx.textBaseline = 'middle';

            layers.forEach((layer, i) => {
                const color = getLayerColor(layer);
                ctx.fillStyle = color;
                ctx.beginPath();
                ctx.arc(35, h - 15 - (layers.length * 25) - 40 + (i * 25), 6, 0, Math.PI * 2);
                ctx.fill();

                ctx.shadowColor = color;
                ctx.shadowBlur = 8;
                ctx.fill();
                ctx.shadowBlur = 0;

                ctx.fillStyle = 'rgba(255, 255, 255, 0.85)';
                ctx.font = 'bold 12px sans-serif';
                ctx.fillText(layer, 55, h - 15 - (layers.length * 25) - 40 + (i * 25));
            });

            // Draw custom legend entries in exported snapshot
            const extraYStart = h - 45;
            // Circular Dependency Red Line
            ctx.strokeStyle = '#ef4444';
            ctx.lineWidth = 2.5;
            ctx.shadowColor = '#ef4444';
            ctx.shadowBlur = 8;
            ctx.beginPath();
            ctx.moveTo(25, extraYStart);
            ctx.lineTo(45, extraYStart);
            ctx.stroke();
            ctx.shadowBlur = 0;

            ctx.fillStyle = '#ef4444';
            ctx.font = 'bold 10px sans-serif';
            ctx.fillText('⚠️ Circular Dependency', 55, extraYStart);

            // Selected Link Orange Line
            const selectY = h - 25;
            ctx.strokeStyle = '#FF9800';
            ctx.lineWidth = 2;
            ctx.shadowColor = '#FF9800';
            ctx.shadowBlur = 6;
            ctx.beginPath();
            ctx.moveTo(25, selectY);
            ctx.lineTo(45, selectY);
            ctx.stroke();
            ctx.shadowBlur = 0;

            ctx.fillStyle = '#FF9800';
            ctx.font = 'bold 10px sans-serif';
            ctx.fillText('🔗 Selected Dependency', 55, selectY);

            ctx.restore();

            // Trigger download
            const dataUrl = canvas.toDataURL('image/png');
            const link = document.createElement('a');
            link.download = `lighthouse-galaxy-level-${'$'}{xpStats.level}.png`;
            link.href = dataUrl;
            link.click();

            // Trigger a re-render to remove the watermark from the live interactive canvas
            fsGraphInstance.render();
        }

        function togglePerfectSimulation() {
            perfectModeEnabled = !perfectModeEnabled;
            const btn = document.getElementById('btn-perfect-sim');
            if (btn) {
                btn.style.background = perfectModeEnabled ? 'rgba(16,185,129,0.3)' : 'rgba(16,185,129,0.15)';
                btn.style.color = perfectModeEnabled ? '#10b981' : '#a7f3d0';
            }
            updateXPPanel();
            if (fsGraphInstance) {
                fsGraphInstance.resetPhysics();
            }
        }

        function showXPTooltip() {
            const bubble = document.getElementById('xp-tooltip-bubble');
            if (bubble) bubble.style.display = 'block';
        }
        function hideXPTooltip() {
            const bubble = document.getElementById('xp-tooltip-bubble');
            if (bubble) bubble.style.display = 'none';
        }
        function toggleXPTooltip(e) {
            if (e) e.stopPropagation();
            const bubble = document.getElementById('xp-tooltip-bubble');
            if (bubble) {
                bubble.style.display = bubble.style.display === 'block' ? 'none' : 'block';
            }
        }
        function updateXPPanel() {
            if (graphData && graphData.nodes) {
                const {xp, perfectModules, level} = calculateXP(graphData.nodes);
                const xpPanel = document.getElementById('fs-xp-panel');
                if(xpPanel) {
                    xpPanel.innerHTML = `
                        <div style="background:rgba(16,185,129,0.1); border:1px solid rgba(16,185,129,0.3); border-radius:8px; padding:12px; text-align:center; box-shadow: inset 0 0 15px rgba(16,185,129,0.1); position:relative;">
                            <div style="font-size:0.7rem; color:#10b981; font-weight:bold;">ARCHITECT LEVEL ${"$"}{level}</div>
                            <div style="font-size:1.5rem; color:white; font-weight:900; margin:4px 0; text-shadow: 0 0 10px rgba(255,255,255,0.3);">${"$"}{xp} XP</div>
                            <div style="font-size:0.65rem; color:rgba(255,255,255,0.7);">${"$"}{perfectModules} Perfect Modules 🎯</div>

                            <div style="position:absolute; top:5px; right:8px; display:inline-block; z-index:100;">
                                <div class="xp-info-trigger" style="cursor:pointer; font-size:0.75rem; color:rgba(255,255,255,0.5); padding:4px; transition:color 0.2s;" onmouseenter="showXPTooltip()" onmouseleave="hideXPTooltip()" onclick="toggleXPTooltip(event)">ℹ️</div>
                                <div id="xp-tooltip-bubble" style="display:none; position:absolute; right:0; top:25px; width:260px; background:#0f172a; border:1px solid #334155; border-radius:8px; padding:12px; box-shadow:0 10px 25px rgba(0,0,0,0.6); z-index:101; text-align:left;">
                                    <div style="font-size:0.75rem; color:#10b981; font-weight:bold; margin-bottom:8px; border-bottom:1px solid #1e293b; padding-bottom:4px;">🛡️ Gamification Protocol</div>
                                    <ul style="margin:0; padding-left:14px; font-size:0.7rem; color:rgba(255,255,255,0.85); display:flex; flex-direction:column; gap:6px; line-height:1.4;">
                                        <li><strong>Module XP:</strong> Health Score &times; 10 (e.g. 95% = 950 XP).</li>
                                        <li><strong>Total XP:</strong> Combined XP of all modules.</li>
                                        <li><strong>Architect Level:</strong> Every 1,000 XP increases your Level by +1.</li>
                                        <li><strong>Perfect Module 🎯:</strong> Any module with a flawless 100% health score.</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    `;
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────────────
        // Historical Trend Chart
        //
        // Data source: `globalHistory` — injected by LighthouseAggregateTask as a JSON
        // array of: { score: Int, fatalCount: Int, couplingDensity: Double, timestamp: String }
        //
        // Polyfill: CanvasRenderingContext2D.roundRect is only available in Chromium 99+
        // and is absent in older embedded WebViews (e.g. Android System WebView < 99).
        // We patch the prototype once so all chart tooltip drawing is safe everywhere.
        if (typeof CanvasRenderingContext2D !== 'undefined' && !CanvasRenderingContext2D.prototype.roundRect) {
            CanvasRenderingContext2D.prototype.roundRect = function(x, y, w, h, r) {
                const radius = Math.min(r || 0, w / 2, h / 2);
                this.beginPath();
                this.moveTo(x + radius, y);
                this.lineTo(x + w - radius, y);
                this.quadraticCurveTo(x + w, y, x + w, y + radius);
                this.lineTo(x + w, y + h - radius);
                this.quadraticCurveTo(x + w, y + h, x + w - radius, y + h);
                this.lineTo(x + radius, y + h);
                this.quadraticCurveTo(x, y + h, x, y + h - radius);
                this.lineTo(x, y + radius);
                this.quadraticCurveTo(x, y, x + radius, y);
                this.closePath();
            };
        }
        // Three series are drawn on a shared canvas with independent Y-axes:
        //   - Health Score  (0–100, green  #10b981)  — left axis
        //   - Total Fatals  (raw,   red    #ef4444)  — right axis (normalised to canvas height)
        //   - Coupling Density (raw, amber #f59e0b)  — right axis (normalised)
        //
        // Formula note:  Coupling Density = Σ(module out-edges) / module count  (average
        // out-degree). A value of 1.0 means every module depends on exactly one other module
        // on average. Aim for < 3.0 in a healthy modular project.
        // ─────────────────────────────────────────────────────────────────────────────
        function drawTrendChart() {
            const canvas = document.getElementById('historical-trend-canvas');
            if (!canvas) return;

            const dpr    = window.devicePixelRatio || 1;
            const parent = canvas.parentElement;
            // Use content-box measurements so we do not feed the parent's border-box width
            // back into the child canvas and trigger ResizeObserver growth loops.
            const W      = parent.clientWidth  || Math.round(parent.getBoundingClientRect().width)  || 600;
            const H      = parent.clientHeight || Math.round(parent.getBoundingClientRect().height) || 220;
            canvas.width  = Math.max(1, Math.round(W * dpr));
            canvas.height = Math.max(1, Math.round(H * dpr));

            const ctx = canvas.getContext('2d');
            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

            // ── Determine light vs dark mode ────────────────────────────────────────
            const isDark     = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
            const bgColor    = isDark ? '#1e293b' : '#f8fafc';
            const gridColor  = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)';
            const textColor  = isDark ? 'rgba(255,255,255,0.55)' : 'rgba(0,0,0,0.45)';
            const labelColor = isDark ? 'rgba(255,255,255,0.8)'  : 'rgba(0,0,0,0.7)';

            // ── Background ──────────────────────────────────────────────────────────
            ctx.fillStyle = bgColor;
            ctx.fillRect(0, 0, W, H);

            const history = (typeof globalHistory !== 'undefined' && Array.isArray(globalHistory))
                ? globalHistory : [];

            // ── No-data state ───────────────────────────────────────────────────────
            if (history.length === 0) {
                ctx.fillStyle = textColor;
                ctx.font = 'bold 14px -apple-system, sans-serif';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillText('No trend data yet.', W / 2, H / 2 - 12);
                ctx.font = '12px -apple-system, sans-serif';
                ctx.fillStyle = isDark ? 'rgba(255,255,255,0.3)' : 'rgba(0,0,0,0.3)';
                ctx.fillText('Run lighthouseAggregate a second time to start tracking.', W / 2, H / 2 + 12);
                return;
            }

            // ── Layout constants ────────────────────────────────────────────────────
            const PAD_LEFT   = 48;  // space for left Y-axis labels (score 0–100)
            const PAD_RIGHT  = 52;  // space for right Y-axis labels (fatals / coupling)
            const PAD_TOP    = 18;
            const PAD_BOTTOM = 36;  // space for X-axis date labels
            const plotW = W - PAD_LEFT - PAD_RIGHT;
            const plotH = H - PAD_TOP  - PAD_BOTTOM;

            // ── Data extraction ─────────────────────────────────────────────────────
            const scores    = history.map(e => typeof e.score         === 'number' ? e.score         : 0);
            const fatals    = history.map(e => typeof e.fatalCount    === 'number' ? e.fatalCount    : 0);
            const couplings = history.map(e => typeof e.couplingDensity === 'number' ? e.couplingDensity : 0);
            const labels    = history.map(e => {
                // Trim ISO timestamp to just the date portion (YYYY-MM-DD)
                const ts = String(e.timestamp || '');
                return ts.substring(0, 10);
            });

            const n = scores.length;

            // ── Normalise fatals & coupling to [0, plotH] independently ─────────────
            const maxFatals    = Math.max(...fatals,    1);
            const maxCoupling  = Math.max(...couplings, 1);

            // Convert absolute values → canvas Y coordinates (0 = top, plotH = bottom)
            const scoreY    = v  => PAD_TOP + plotH - (Math.min(Math.max(v, 0), 100) / 100) * plotH;
            const fatalY    = v  => PAD_TOP + plotH - (v / maxFatals)   * plotH * 0.85; // 85% headroom
            const couplingY = v  => PAD_TOP + plotH - (v / maxCoupling) * plotH * 0.85;
            const xAt       = i  => PAD_LEFT + (n === 1 ? plotW / 2 : (i / (n - 1)) * plotW);

            // ── Grid lines (score axis: 0, 25, 50, 75, 100) ─────────────────────────
            ctx.save();
            ctx.strokeStyle = gridColor;
            ctx.lineWidth   = 1;
            ctx.setLineDash([4, 4]);
            [0, 25, 50, 75, 100].forEach(tick => {
                const y = scoreY(tick);
                ctx.beginPath();
                ctx.moveTo(PAD_LEFT, y);
                ctx.lineTo(PAD_LEFT + plotW, y);
                ctx.stroke();
            });
            ctx.setLineDash([]);
            ctx.restore();

            // ── Left Y-axis labels (Health Score %) ─────────────────────────────────
            ctx.fillStyle   = '#10b981';
            ctx.font        = 'bold 10px -apple-system, sans-serif';
            ctx.textAlign   = 'right';
            ctx.textBaseline = 'middle';
            [0, 25, 50, 75, 100].forEach(tick => {
                ctx.fillText(tick + '%', PAD_LEFT - 6, scoreY(tick));
            });

            // ── Right Y-axis labels (Fatals — raw count) ────────────────────────────
            ctx.fillStyle   = '#ef4444';
            ctx.textAlign   = 'left';
            [0, Math.round(maxFatals / 2), maxFatals].forEach(tick => {
                ctx.fillText(String(tick), PAD_LEFT + plotW + 6, fatalY(tick));
            });

            // ── Coupling density guide line label ────────────────────────────────────
            // Draw right-axis coupling max label in amber, offset slightly to avoid overlap
            ctx.fillStyle   = '#f59e0b';
            ctx.fillText(maxCoupling.toFixed(1), PAD_LEFT + plotW + 6, PAD_TOP + 8);

            // ── Helper: draw a smooth line series ────────────────────────────────────
            function drawLine(yFn, color, dashed) {
                ctx.save();
                ctx.strokeStyle = color;
                ctx.lineWidth   = 2;
                ctx.lineJoin    = 'round';
                ctx.lineCap     = 'round';
                if (dashed) ctx.setLineDash([6, 4]);
                ctx.shadowColor = color;
                ctx.shadowBlur  = 8;
                ctx.beginPath();
                for (let i = 0; i < n; i++) {
                    const x = xAt(i);
                    const y = yFn(history[i]);
                    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                }
                ctx.stroke();
                ctx.setLineDash([]);
                ctx.restore();
            }

            // ── Helper: draw subtle area fill under a series ─────────────────────────
            function drawArea(yFn, color) {
                ctx.save();
                ctx.beginPath();
                for (let i = 0; i < n; i++) {
                    const x = xAt(i);
                    const y = yFn(history[i]);
                    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                }
                ctx.lineTo(xAt(n - 1), PAD_TOP + plotH);
                ctx.lineTo(xAt(0),     PAD_TOP + plotH);
                ctx.closePath();
                ctx.fillStyle = color;
                ctx.fill();
                ctx.restore();
            }

            // ── Draw series ──────────────────────────────────────────────────────────
            // Area fills (rendered first, under the lines)
            drawArea(e => scoreY(e.score),            'rgba(16,185,129,0.08)');
            drawArea(e => fatalY(e.fatalCount),       'rgba(239,68,68,0.06)');
            drawArea(e => couplingY(e.couplingDensity),'rgba(245,158,11,0.06)');

            // Lines
            drawLine(e => scoreY(e.score),            '#10b981', false);
            drawLine(e => fatalY(e.fatalCount),       '#ef4444', false);
            drawLine(e => couplingY(e.couplingDensity),'#f59e0b', true); // dashed to distinguish

            // ── Data-point dots ──────────────────────────────────────────────────────
            [[scores, scoreY, '#10b981'], [fatals, fatalY, '#ef4444'], [couplings, couplingY, '#f59e0b']].forEach(([arr, yFn, color]) => {
                for (let i = 0; i < n; i++) {
                    const x  = xAt(i);
                    const y  = yFn(arr[i]);
                    ctx.beginPath();
                    ctx.arc(x, y, 3, 0, Math.PI * 2);
                    ctx.fillStyle   = color;
                    ctx.shadowColor = color;
                    ctx.shadowBlur  = 6;
                    ctx.fill();
                    ctx.shadowBlur  = 0;
                }
            });

            // ── Hover tooltip via mousemove ──────────────────────────────────────────
            // Uses a data attribute on the canvas to avoid registering multiple listeners
            if (!canvas.dataset.tooltipBound) {
                canvas.dataset.tooltipBound = '1';
                canvas.addEventListener('mousemove', ev => {
                    const r  = canvas.getBoundingClientRect();
                    const mx = ev.clientX - r.left;

                    // Find nearest data point by X distance
                    let best = 0;
                    let bestDist = Infinity;
                    for (let i = 0; i < n; i++) {
                        const d = Math.abs(xAt(i) - mx);
                        if (d < bestDist) { bestDist = d; best = i; }
                    }

                    if (bestDist > plotW / n + 8) return; // outside plot area

                    // Clear + redraw chart, then overlay the tooltip
                    drawTrendChart();
                    const h  = history[best];
                    const tx = xAt(best);

                    // Vertical guide line
                    ctx.save();
                    ctx.strokeStyle = isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.15)';
                    ctx.lineWidth   = 1;
                    ctx.setLineDash([3, 3]);
                    ctx.beginPath();
                    ctx.moveTo(tx, PAD_TOP);
                    ctx.lineTo(tx, PAD_TOP + plotH);
                    ctx.stroke();
                    ctx.setLineDash([]);
                    ctx.restore();

                    // Tooltip box
                    const tipW   = 180;
                    const tipH   = 76;
                    const tipX   = Math.min(tx + 10, W - tipW - 4);
                    const tipY   = PAD_TOP + 4;
                    const radius = 8;

                    ctx.save();
                    ctx.fillStyle   = isDark ? 'rgba(15,23,42,0.92)' : 'rgba(255,255,255,0.95)';
                    ctx.strokeStyle = isDark ? '#334155' : '#e2e8f0';
                    ctx.lineWidth   = 1;
                    ctx.shadowColor = 'rgba(0,0,0,0.3)';
                    ctx.shadowBlur  = 12;
                    ctx.beginPath();
                    ctx.roundRect(tipX, tipY, tipW, tipH, radius);
                    ctx.fill();
                    ctx.stroke();
                    ctx.shadowBlur = 0;

                    // Tooltip text
                    ctx.textAlign    = 'left';
                    ctx.textBaseline = 'middle';
                    ctx.font = 'bold 10px -apple-system, sans-serif';
                    ctx.fillStyle = labelColor;
                    ctx.fillText(h.timestamp ? String(h.timestamp).substring(0, 10) : 'Build ' + (best + 1), tipX + 10, tipY + 14);

                    const rows = [
                        ['#10b981', 'Health', (h.score ?? 0) + '%'],
                        ['#ef4444', 'Fatals', String(h.fatalCount ?? 0)],
                        ['#f59e0b', 'Coupling', (h.couplingDensity ?? 0).toFixed(2) + ' avg deps'],
                    ];
                    rows.forEach(([color, label, val], ri) => {
                        const ry = tipY + 30 + ri * 16;
                        ctx.beginPath();
                        ctx.arc(tipX + 14, ry, 4, 0, Math.PI * 2);
                        ctx.fillStyle = color;
                        ctx.fill();
                        ctx.font      = '10px -apple-system, sans-serif';
                        ctx.fillStyle = labelColor;
                        ctx.fillText(label + ':', tipX + 24, ry);
                        ctx.font      = 'bold 10px -apple-system, sans-serif';
                        ctx.fillText(val, tipX + 80, ry);
                    });
                    ctx.restore();
                });

                canvas.addEventListener('mouseleave', () => drawTrendChart());
            }

            // ── X-axis: show up to 6 evenly spaced date labels ───────────────────────
            ctx.fillStyle    = textColor;
            ctx.font         = '9px -apple-system, sans-serif';
            ctx.textAlign    = 'center';
            ctx.textBaseline = 'top';
            const tickCount  = Math.min(n, 6);
            for (let t = 0; t < tickCount; t++) {
                const i = Math.round(t * (n - 1) / Math.max(tickCount - 1, 1));
                ctx.fillText(labels[i], xAt(i), PAD_TOP + plotH + 6);
            }

            // ── Chart border ─────────────────────────────────────────────────────────
            ctx.strokeStyle = gridColor;
            ctx.lineWidth   = 1;
            ctx.strokeRect(PAD_LEFT, PAD_TOP, plotW, plotH);
        }

        document.addEventListener('DOMContentLoaded', () => {
            initGraphData();
            // Deep-copy each link object so sandbox mutations never corrupt the backup.
            originalLinks = graphData.links.map(l => ({ ...l }));

            // Defer graph initialisation by one rAF so the browser has completed its
            // first layout pass and getBoundingClientRect() returns real dimensions.
            requestAnimationFrame(() => {
                const canvas = document.getElementById('module-graph-canvas');
                if(canvas && graphData && graphData.nodes && graphData.nodes.length > 0) {
                    graphInstance = new ModuleGraph(canvas, false);
                }
            });

            // Build Legend with Interactive Layer Isolation Focus!
            const layersSet = new Set(graphData.nodes.map(n => n.layer));
            const legendHtml = [...layersSet].sort().map(k =>
                `<div class="legend-row" data-layer="${"$"}{k}" style="display:flex; align-items:center; gap:8px; margin-bottom:6px; cursor:pointer; padding:4px 8px; border-radius:6px; transition:background 0.2s;" title="Filter by layer: ${"$"}{k}">
                    <div style="width:10px;height:10px;border-radius:50%;background:${"$"}{getLayerColor(k)};box-shadow: 0 0 8px ${"$"}{getLayerColor(k)};"></div>
                    <span style="font-size:0.75rem;color:rgba(255,255,255,0.8); font-weight:bold;">${"$"}{k}</span>
                </div>`
            ).join('') + `
                <div style="margin-top:10px; padding-top:10px; border-top:1px solid #1e293b; display:flex; flex-direction:column; gap:6px;">
                    <div style="display:flex; align-items:center; gap:8px;">
                        <div style="width:14px; height:3px; background:#ef4444; box-shadow: 0 0 6px #ef4444; border-radius:1px;"></div>
                        <span style="font-size:0.7rem; color:#ef4444; font-weight:bold;">⚠️ Circular Dependency (Cycle)</span>
                    </div>
                    <div style="display:flex; align-items:center; gap:8px;">
                        <div style="width:14px; height:2px; background:rgba(255,152,0,0.8); box-shadow: 0 0 6px #FF9800; border-radius:1px;"></div>
                        <span style="font-size:0.7rem; color:#FF9800; font-weight:bold;">🔗 Selected Link</span>
                    </div>
                </div>
            `;
            const legE = document.getElementById('fs-layer-legend');
            if(legE) {
                legE.innerHTML = legendHtml;
                legE.querySelectorAll('.legend-row').forEach(row => {
                    row.addEventListener('click', () => {
                        const layer = row.getAttribute('data-layer');
                        if (activeIsolatedLayer === layer) {
                            activeIsolatedLayer = null;
                            row.style.background = 'none';
                        } else {
                            activeIsolatedLayer = layer;
                            legE.querySelectorAll('.legend-row').forEach(r => r.style.background = 'none');
                            row.style.background = 'rgba(255,255,255,0.08)';
                        }
                    });
                    row.addEventListener('mouseenter', () => {
                        if (activeIsolatedLayer !== row.getAttribute('data-layer')) {
                            row.style.background = 'rgba(255,255,255,0.04)';
                        }
                    });
                    row.addEventListener('mouseleave', () => {
                        if (activeIsolatedLayer !== row.getAttribute('data-layer')) {
                            row.style.background = 'none';
                        }
                    });
                });
            }

            // Build XP Panel
            updateXPPanel();

            // ── Historical Trend Chart ─────────────────────────────────────────────────
            drawTrendChart();
            // Re-draw on resize so the chart is always sharp & correctly proportioned
            const trendCanvas = document.getElementById('historical-trend-canvas');
            if (trendCanvas) {
                new ResizeObserver(() => drawTrendChart()).observe(trendCanvas.parentElement);
            }

            const btnCycles = document.getElementById('btn-cycles');
            if (btnCycles) {
                btnCycles.addEventListener('click', (e) => {
                    e.preventDefault();
                    toggleCyclesFilter();
                });
            }

            document.addEventListener('keydown', e => {
                if (document.getElementById('graph-fullscreen-overlay').style.display === 'flex') {
                    if (e.key === 'Escape') closeFullscreenGraph();
                    if (e.key.toLowerCase() === 'f') graphZoomFit();
                    if (e.key.toLowerCase() === 'l') toggleLayerView();
                    if (e.key.toLowerCase() === 'r') resetInlineGraph();
                }
            });
        });
        // -----------------------------------------------------------------------------
    """
}
