---
name: Serein Day
colors:
  surface: '#fef7ff'
  surface-dim: '#dfd7e3'
  surface-bright: '#fef7ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f9f1fd'
  surface-container: '#f3ebf7'
  surface-container-high: '#ede6f1'
  surface-container-highest: '#e7e0eb'
  on-surface: '#1d1a22'
  on-surface-variant: '#494551'
  inverse-surface: '#322f37'
  inverse-on-surface: '#f6eefa'
  outline: '#7a7582'
  outline-variant: '#cbc4d2'
  surface-tint: '#6750a4'
  primary: '#4f378a'
  on-primary: '#ffffff'
  primary-container: '#6750a4'
  on-primary-container: '#e0d2ff'
  inverse-primary: '#cfbcff'
  secondary: '#625b71'
  on-secondary: '#ffffff'
  secondary-container: '#e8def9'
  on-secondary-container: '#686177'
  tertiary: '#633b48'
  on-tertiary: '#ffffff'
  tertiary-container: '#7d5260'
  on-tertiary-container: '#ffcbda'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e9ddff'
  primary-fixed-dim: '#cfbcff'
  on-primary-fixed: '#22005d'
  on-primary-fixed-variant: '#4f378a'
  secondary-fixed: '#e8def9'
  secondary-fixed-dim: '#ccc2dc'
  on-secondary-fixed: '#1e192b'
  on-secondary-fixed-variant: '#4a4358'
  tertiary-fixed: '#ffd9e3'
  tertiary-fixed-dim: '#eeb8c8'
  on-tertiary-fixed: '#31111d'
  on-tertiary-fixed-variant: '#633b48'
  background: '#fef7ff'
  on-background: '#1d1a22'
  surface-variant: '#e7e0eb'
typography:
  display-lg:
    fontFamily: Roboto Flex
    fontSize: 57px
    fontWeight: '400'
    lineHeight: 64px
    letterSpacing: -0.25px
  display-md:
    fontFamily: Roboto Flex
    fontSize: 45px
    fontWeight: '400'
    lineHeight: 52px
    letterSpacing: 0px
  display-sm:
    fontFamily: Roboto Flex
    fontSize: 36px
    fontWeight: '400'
    lineHeight: 44px
    letterSpacing: 0px
  headline-lg:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '500'
    lineHeight: 40px
    letterSpacing: 0px
  headline-md:
    fontFamily: Roboto Flex
    fontSize: 28px
    fontWeight: '500'
    lineHeight: 36px
    letterSpacing: 0px
  headline-sm:
    fontFamily: Roboto Flex
    fontSize: 24px
    fontWeight: '500'
    lineHeight: 32px
    letterSpacing: 0px
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0px
  title-md:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.15px
  title-sm:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  body-sm:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.4px
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
  label-sm:
    fontFamily: Roboto Flex
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  spacing-xxs: 0.125rem
  spacing-xs: 0.25rem
  spacing-sm: 0.5rem
  spacing-md: 1rem
  spacing-lg: 1.5rem
  spacing-xl: 2rem
  spacing-xxl: 3rem
  connected-gap: 0.1875rem
  screen-margin-horizontal: 1rem
  card-padding: 1.25rem
  button-height: 3.5rem
---

## Brand & Style

This design system delivers an intimate, serene, and emotionally resonant experience tailored for milestone tracking, anniversaries, and personal countdowns on Android. The aesthetic fuses the principles of Material 3 Expressive with gentle, tactile humanism—shifting countdown tracking away from stress-inducing urgency toward reflective anticipation and celebration.

### Emotional Landscape & Persona
- **Poise & Tenderness:** Celebrations and commemorations are handled with visual calm and clarity rather than overwhelming gamification.
- **Android Native Expression:** Fully embraces dynamic theming mechanics, expressive tonal surface progression, squircle geometry, and fluid haptics.
- **Hierarchy of Importance:** Events transition effortlessly from everyday reminders (soft tonal surfaces) to focal lifetime anniversaries (accented expressive containers with rich typography).

## Colors

The palette is derived from the classic Material 3 dynamic seed fallback in rich purple, utilizing tonal surface stepping rather than harsh drop shadows to communicate boundaries and layering.

### Palette Roles
- **Primary (`#6750A4`) & Primary Container (`#EADDFF`):** Reserved for core focal items, such as primary action buttons, today's headline countdown, and key milestone badges.
- **Secondary Container (`#E8DEF8`):** Handles supportive metadata, secondary categories, and intermediate countdown periods (weeks/months).
- **Tertiary Container (`#FFD8E4`):** Expressive highlight for sentimental events, love anniversaries, and romantic or commemorative counters.
- **Tonal Surface Hierarchy:**
  - `surface` (`#FEF7FF`): Scaffolding background canvas.
  - `surfaceContainerLow` (`#F7F2FA`): Subtle group backgrounds and resting list modules.
  - `surfaceContainer` (`#F3EDF7`): Standard card base and search/input backplates.
  - `surfaceContainerHigh` (`#ECE6F0`): Floating interactive sheets, popups, and elevated dialogs.
  - `surfaceContainerHighest` (`#E6E0E9`): Inactive switches, divider states, and drag indicators.
- **Outlines:** Use `outlineVariant` (`#CAC4D0`) for soft hairline separations and `outline` (`#79747E`) exclusively for input borders and icon strokes requiring AA contrast.

## Typography

Typography relies on **Roboto Flex**, leaning into variable axis weights to balance dense metric visualization (days remaining, dates) with calming editorial headers.

### Application Rules
- **Large Day Digits:** Countdown numbers use `display-lg` (57px) or `display-md` (45px) in tabular figure settings (`font-variant-numeric: tabular-nums;`) to prevent layout jump during live ticking updates.
- **Editorial Card Titles:** Event titles use `title-lg` or `headline-sm` with a medium-to-semi-bold weight to command hierarchy without feeling aggressive.
- **Unit Annotations:** Accompanying day, hour, and second indicators employ `label-sm` in all-caps with generous letter-spacing (+0.5px) rendered in `onSurfaceVariant`.

## Layout & Spacing

The layout is built around the modern flagship portrait base screen size of **412dp × 892dp** with an adaptive 4-column fluid mobile grid.

### Layout Specs
- **Margins & Gutters:** 16dp outer screen margin padding, with 12dp column gutters.
- **Connected List Rhythm:** Grouped cards adhere to an explicit stacked system: an external top/bottom envelope with 3dp intra-item gaps (`connected-gap`), maintaining continuous visual clustering for common categories.
- **Safe Area Anchors:** Bottom-docked primary action spaces allow 80dp clearance above the Android system navigation gesture bar.

## Elevation & Depth

This design system avoids heavy drop shadows in favor of Material 3 Expressive **tonal layering**, using tint transitions and ambient diffusion to express elevation tiers.

### Elevation Levels
- **Level 0 (Base Canvas):** `surface` (`#FEF7FF`), un-elevated, flat backdrop.
- **Level 1 (Resting Cards & Connected Groups):** `surfaceContainerLow` (`#F7F2FA`), 0px shadow offset. Separation is maintained strictly via tonal change or `outlineVariant` border strokes when set against identical backgrounds.
- **Level 2 (Interactive Floating Elements & FAB):** `surfaceContainerHigh` (`#ECE6F0`) with an ultra-soft ambient shadow: `box-shadow: 0px 4px 12px rgba(33, 0, 93, 0.06), 0px 1px 4px rgba(33, 0, 93, 0.04)`.
- **Level 3 (Modal Bottom Sheets & Menus):** `surfaceContainerHighest` (`#E6E0E9`) paired with a scrim of `rgba(29, 27, 32, 0.32)` and ambient drop: `box-shadow: 0px 8px 24px rgba(33, 0, 93, 0.12)`.

## Shapes

Shapes feature high-curvature corners characteristic of Material 3 Expressive.

### Geometry Values
- **Cards & Surfaces:** Standard standalone countdown cards feature a uniform **20dp (1.25rem)** squircle corner radius.
- **Pill Elements:** Buttons, chips, search inputs, and status tags use full-pill styling (`rounded-full` / 999dp) with standardized heights (56dp for primary buttons).
- **Floating Action Buttons (FAB):** 56dp square with **16dp (1rem)** rounded corners.
- **Connected List Items:**
  - First item: Top corners 28dp, bottom corners 8dp.
  - Middle items: All corners 8dp.
  - Last item: Top corners 8dp, bottom corners 28dp.
  - Single isolated item: All corners 28dp.

## Components

### 1. Countdown & Milestone Hero Cards
- **Structure:** Standalone 20dp rounded surface filled with `primaryContainer` or `surfaceContainer`.
- **Content:** Header display number (`display-lg` in tabular numerals), tiny units baseline label (`label-sm`), followed by celebration name (`title-lg`).
- **Internal Padding:** 20dp top/sides, 24dp bottom.

### 2. Buttons
- **Primary CTA:** 56dp fixed height, full pill shape (999dp). Filled with `primary` (`#6750A4`), label in `onPrimary` (`#FFFFFF`, `label-lg`). Horizontal padding: 24dp.
- **Tonal Button:** 56dp fixed height, full pill shape. Filled with `secondaryContainer` (`#E8DEF8`), label in `onSecondaryContainer` (`#1D192B`).
- **Floating Action Button (FAB):** 56dp × 56dp with 16dp corner radius. Background `primaryContainer`, icon tint `onPrimaryContainer`. Elevated at Level 2.

### 3. Connected Milestone Lists
- **Container:** Borderless stack spaced by 3dp vertical gap.
- **Corner Morphing:** Outer ends curve at 28dp; internal adjacent corners curve at 8dp.
- **Tonal Surface:** Resting state `surfaceContainerLow`; pressed ripple state `surfaceContainerHighest`.
- **Layout:** Leading custom anniversary icon or avatar (40dp circle), middle stacked title + sub-date, trailing remaining day pill tag (`tertiaryContainer` fill with `onTertiaryContainer` text).

### 4. Chips & Filters
- **Form:** 32dp height, 8dp corner radius.
- **States:**
  - Selected: Filled with `secondaryContainer`, borderless, text `onSecondaryContainer`, accompanied by a leading checkmark icon (18dp).
  - Unselected: Surface flat, 1dp hairline border in `outlineVariant`, text `onSurfaceVariant`.

### 5. Input Fields (Event Creation)
- **Filled Text Field:** 56dp height, `surfaceContainerHighest` fill, top-only rounding (12dp) with active indicator bottom border (2dp `primary` on focus).
- **Outlined Text Field:** 56dp height, fully rounded pill shape (28dp), 1dp border `outlineVariant` (2dp `primary` on focus). Label floats seamlessly onto border band.

### 6. Date & Anniversary Wheel Pickers
- Embedded within a bottom sheet (`surfaceContainerHigh`, 28dp top radius). Active highlight row rendered as a 48dp high full-width pill in `surfaceContainerHighest`.