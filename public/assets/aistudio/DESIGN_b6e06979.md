---
name: Obsidian Kinetic
colors:
  surface: '#111319'
  surface-dim: '#111319'
  surface-bright: '#373940'
  surface-container-lowest: '#0c0e14'
  surface-container-low: '#191b22'
  surface-container: '#1e1f26'
  surface-container-high: '#282a30'
  surface-container-highest: '#33343b'
  on-surface: '#e2e2eb'
  on-surface-variant: '#d8c3ad'
  inverse-surface: '#e2e2eb'
  inverse-on-surface: '#2e3037'
  outline: '#a08e7a'
  outline-variant: '#534434'
  surface-tint: '#ffb95f'
  primary: '#ffc174'
  on-primary: '#472a00'
  primary-container: '#f59e0b'
  on-primary-container: '#613b00'
  inverse-primary: '#855300'
  secondary: '#7bd0ff'
  on-secondary: '#00354a'
  secondary-container: '#00a6e0'
  on-secondary-container: '#00374d'
  tertiary: '#56e5a9'
  on-tertiary: '#003824'
  tertiary-container: '#30c88f'
  on-tertiary-container: '#004e34'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffddb8'
  primary-fixed-dim: '#ffb95f'
  on-primary-fixed: '#2a1700'
  on-primary-fixed-variant: '#653e00'
  secondary-fixed: '#c4e7ff'
  secondary-fixed-dim: '#7bd0ff'
  on-secondary-fixed: '#001e2c'
  on-secondary-fixed-variant: '#004c69'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#111319'
  on-background: '#e2e2eb'
  surface-variant: '#33343b'
typography:
  headline-xl:
    fontFamily: Geist
    fontSize: 40px
    fontWeight: '600'
    lineHeight: 48px
    letterSpacing: -0.03em
  headline-xl-mobile:
    fontFamily: Geist
    fontSize: 30px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.025em
  headline-lg:
    fontFamily: Geist
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Geist
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 26px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 22px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-md:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-lg:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Geist
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.05em
  tabular-time:
    fontFamily: Geist
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-lg: 1.5rem
  margin: 1rem
  margin-md: 1.5rem
  margin-lg: 2.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1.25rem
  space-xl: 2rem
---

## Brand & Style

This design system is tailored for an ultra-focused, high-velocity day-planning and calendar environment. The brand personality is razor-sharp, quiet, and hyper-efficient. It strips away productivity theater, telemetry overload, and dense corporate widgets in favor of instantaneous legibility and rapid keyboard- or gesture-driven capture.

The aesthetic fuses **Precision Minimalism** with subtle **Tactile Neomorphism / Dark HUD** clarity:
- **Surface Depth:** Immersive obsidian dark-mode canvases that eliminate eye fatigue and frame tasks like bright physical tokens.
- **Focus Mechanism:** High-contrast amber-gold energetic pulses reserved strictly for active states, vital time indicators, and high-priority interactions.
- **Zero Bloat:** Interfaces favor single-keystroke workflows, micro-interactions with immediate visual feedback (<100ms response feel), and generous structural breathing room that lets users assess their entire day in under two seconds.

## Colors

The palette establishes extreme contrast while preserving eye comfort via a stepped obsidian background architecture.

### Color Tiers & Roles
- **Canvas Base (`#0c0e14`):** The foundational obsidian canvas; void-like, absorbing, and unobtrusive.
- **Surface Elevation 1 (`#121620`):** Elevated panels, calendar time-block columns, and secondary navigation shells.
- **Surface Elevation 2 (`#1a202c`):** Interactive cards, hover states, floating menus, and popovers.
- **Primary Accent (`#f59e0b` - Radiant Amber):** Reserved exclusively for active time markers, the primary action button (FAB / Quick Add), today's date focus, and urgency highlights. Never used for decorative washes or large background blocks.
- **Secondary Accent (`#38bdf8` - Hyper Sky):** Denotes secondary time categories, informational badges, and external calendar integrations.
- **Tertiary Accent (`#10b981` - Mint Signal):** Completed task indicators, progress checks, and available focus windows.
- **Typography & Structural Contrast:**
  - `text-primary`: `#f8fafc` (high contrast, crisp white-slate)
  - `text-secondary`: `#94a3b8` (subdued metadata, timestamps, shortcut hints)
  - `text-muted`: `#475569` (empty slots, past hours, structural dividers)
  - `border-hairline`: `rgba(255, 255, 255, 0.07)` (low-opacity edge definition)

## Typography

Geist provides mechanical precision, ultra-clean geometric neutrality, and tabular figure support for timecodes and agenda slots. 

### Operational Rules
- **Tabular Numerals (`font-feature-settings: 'tnum'`):** Compulsory on all time representations (`09:00 - 09:45`), date counters, and keyboard shortcut glyphs to eliminate horizontal layout jitter when scrubbing through timelines.
- **Tight Letter Spacing:** Negative tracking applied to larger headlines tightens visual weight and reinforces the industrial, low-friction HUD feel.
- **Strict Hierarchy:** Never stack more than two typographic levels in a single card (e.g., `label-lg` for task title, `tabular-time` for temporal constraints).

## Layout & Spacing

The layout model balances generous edge margins with compact, functional component spacing to maintain an airy canvas without sacrificing density.

### Breakpoints & Adaptive Rhythm
- **Mobile (< 768px):** Single-column timeline stream. `margin: 1rem`, `gutter: 1rem`. Floating Quick-Add anchor sits bottom-right. The daily agenda occupies 100% of the viewport width with quick swipe-to-defer mechanics.
- **Tablet (768px - 1024px):** Asymmetric 2-column view (Left: 300px contextual mini-month and task inbox; Right: expanding Day/3-Day timeline). `margin-md: 1.5rem`.
- **Desktop (> 1024px):** Fixed-inbox master rail (320px) paired with a dynamic fluid schedule center-stage and a minimal keyboard shortcut/telemetry strip (240px). `margin-lg: 2.5rem`, `gutter-lg: 1.5rem`.

### Spatial Constraints
- Spacing strictly follows an 8pt base grid with 4pt half-steps for micro-components (pills, badges, and inline controls).
- Every card interior enforces `space-md` (`0.75rem`) for compact density, while lists enforce `space-xs` (`0.25rem`) between recurring task items.

## Elevation & Depth

Visual separation avoids heavy, muddy drop shadows in favor of low-contrast ghost outlines and disciplined tonal layering.

### Surface Tiers
- **Tier 0 (Base Canvas):** `#0c0e14`. Completely non-interactive backdrop.
- **Tier 1 (Structural Sections):** `#121620` with a `1px solid rgba(255, 255, 255, 0.05)` border.
- **Tier 2 (Interactive Modules):** `#1a202c` with `1px solid rgba(255, 255, 255, 0.08)`.
- **Tier 3 (Floating Overlays / Modals):** `#1a202c` elevated by a pinpoint amber rim `1px solid rgba(245, 158, 11, 0.25)` and an ambient diffuse shadow (`box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.6)`).

### Active States & Glow
To communicate velocity and immediate active focus without visual clutter:
- Selected/Current Time Block receives a soft outer luminescence: `box-shadow: 0 0 16px -2px rgba(245, 158, 11, 0.15)`.
- Live time needle features a pulsing hairline: `1px solid #f59e0b` with an amber ping indicator (`4px` diameter).

## Shapes

The interface embraces a hyper-sleek pill and pebble language (`roundedness: 3`).

- **Buttons & Chips:** Full circular radiuses (`rounded-full` / `9999px`) deliver a soft, tactile contrast against the dark background.
- **Task & Event Cards:** Large pill-softened rectangles (`rounded-xl` at `1.5rem` / `24px`) that feel like physical schedule tokens.
- **Input Fields & Command Palettes:** Fully rounded pill-shaped enclosures (`9999px`) creating distinct capsules for inline text capture.
- **Current Hour Line Indicator:** Rounded terminal ends to avoid harsh, unfinished vector edges.

## Components

### Buttons
- **Primary Action (Quick Add / Focus):** Amber gold (`#f59e0b`) background, ultra-dark text (`#0c0e14`), font weight 600. Fully rounded pill. Padding: `0.5rem 1.25rem`. Micro-hover: `brightness(1.08)` and `scale(1.02)`.
- **Secondary / Ghost:** Transparent background, `1px solid rgba(255, 255, 255, 0.12)`, text `#f8fafc`. Hover shifts background to `rgba(255, 255, 255, 0.05)`.
- **Icon Buttons:** `36px x 36px` circular hit targets, neutral `#1a202c` fill with `text-secondary` icons, brightening to `text-primary` on interaction.

### Chips & Sleek Pills
- Height: `26px`. Padding: `0 10px`. 
- Categorical chips use a `rgba(255, 255, 255, 0.04)` fill with hairline borders. Selected chips flip to solid `#f59e0b` text with a subtle `rgba(245, 158, 11, 0.12)` amber tint background.
- Include single-character or minimal keyboard shortcut tags (`Q`, `W`, `E`) in `label-sm` font.

### Lists & Timeline Entries
- Borderless list rows separated by `0.25rem` vertical gaps.
- Hovering a task row instantly displays speed-action icons (Reschedule, Delete, Pin) at the right edge without disrupting text alignment.
- Completed state: task text switches to `text-muted` with an instantaneous strike-through animation, and the row collapses down to `80%` opacity.

### Checkboxes & Completion Radios
- Rounded circular checkboxes (`18px x 18px`), `1.5px` border in `rgba(255, 255, 255, 0.2)`.
- On click: instant pop to `#10b981` (Mint Signal) checkmark with a rapid 120ms spring scale.

### Input Fields & Command Bar
- Zero-label, single-line input pills designed for rapid natural language parsing (`"Team Sync tomorrow at 3pm"`).
- Background `#121620`, border `1px solid rgba(255, 255, 255, 0.08)`.
- Focused state transitions to `1px solid #f59e0b` with zero layout shift. Placeholder text in `#475569`.

### Cards (Task & Event Blocks)
- Surface: `#121620` resting, `#1a202c` hovered.
- Border: `1px solid rgba(255, 255, 255, 0.06)`.
- Left-edge accent strip (`3px` width, rounded cap) indicating category color (Amber, Sky, Mint).
- Internal layout: single row layout with task label on the left and `tabular-time` duration badge on the right.

### Command Palette (Spotlight Interface)
- Modal floating at viewport top-third, max-width `560px`.
- Obsidian backing (`#121620`) wrapped with `1px solid rgba(245, 158, 11, 0.3)`.
- Instant search results rendered as condensed pill items with dedicated keyboard return cues (`↵`).