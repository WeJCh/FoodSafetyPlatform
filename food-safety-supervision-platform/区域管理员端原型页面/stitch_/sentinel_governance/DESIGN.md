# Design System Strategy: The Sovereign Oversight

## 1. Overview & Creative North Star
**The Creative North Star: "The Digital Ledger"**
This design system moves away from the "web portal" aesthetic and toward a high-integrity "Digital Ledger." The goal is to convey absolute regulatory authority through **Architectural Precision**. We reject the clutter of traditional admin templates in favor of an editorial layout that feels as permanent and reliable as a physical government record.

We break the "standard" look by utilizing **intentional density**. While most modern apps crave "white space," a regulator needs "clarity through structure." We achieve this through a rigid, layered hierarchy that uses tonal depth rather than lines to organize complex food safety data. It is a system of "Soft Power"—authoritative and firm, but visually sophisticated.

---

## 2. Colors: Tonal Authority
Color here is not decorative; it is functional and hierarchical. We utilize a "No-Line" philosophy to maintain a premium, clean aesthetic.

*   **Primary Logic:** We use `primary` (#002660) and `primary_container` (#003a8c) to establish a "Deep Sea" anchor. This is the color of the law and the institution.
*   **The "No-Line" Rule:** Designers are strictly prohibited from using 1px solid borders to separate sections. Boundaries are defined by the transition from `surface` (#f7f9fc) to `surface_container_low` (#f2f4f7). If two areas must be distinct, change the background token, do not draw a line.
*   **Surface Hierarchy & Nesting:** 
    *   **Level 0 (Base):** `surface` (#f7f9fc) – The foundational canvas.
    *   **Level 1 (Cards):** `surface_container_lowest` (#ffffff) – Used for high-density data cards to provide maximum contrast for Chinese typography.
    *   **Level 2 (Insets):** `surface_container_high` (#e6e8eb) – Used for embedded search bars or internal groupings within a card.
*   **Signature Textures:** For high-level summaries, apply a subtle linear gradient from `primary` to `primary_container` (at 135 degrees). This creates a "weighted" header that feels significant and unmovable.

---

## 3. Typography: Editorial Legibility
We use a dual-font strategy to balance international standards with Chinese character clarity.

*   **Public Sans (Display/Headline):** This provides a structural, slightly geometric feel for numbers and headers, conveying a "Global Standard" vibe.
*   **Inter (Title/Body/Label):** Chosen for its exceptional legibility at small sizes (0.75rem - 0.875rem), crucial for high-density tables and regulatory fine print.
*   **The Authority Scale:**
    *   **Headlines (`headline-md`):** Used for section titles. Always paired with a `primary` color token to act as a visual anchor.
    *   **Data Labels (`label-md`):** Used for table headers. These should be in `on_surface_variant` (#434651) and strictly All-Caps for Latin characters to create a "form-like" official feel.

---

## 4. Elevation & Depth: Tonal Layering
Traditional shadows feel too "floaty" for a government platform. We use **Atmospheric Weight**.

*   **The Layering Principle:** Instead of shadows, stack `surface_container_lowest` objects on top of `surface_container_low` backgrounds. The subtle 2% shift in brightness is enough for the human eye to perceive depth without the "fuzziness" of a shadow.
*   **Ambient Shadows:** For floating Modals or Popovers, use a shadow with a 32px blur and 4% opacity of the `on_surface` color. It should feel like a soft glow of light, not a drop shadow.
*   **The "Ghost Border" Fallback:** If a UI element (like a search input) risks disappearing, use a "Ghost Border": 1px `outline_variant` (#c3c6d3) at **15% opacity**.
*   **Glassmorphism:** Use `backdrop-blur: 12px` on the sidebar or header overlays with a semi-transparent `surface` color to allow the "content energy" to peek through while maintaining focus.

---

## 5. Components: Precision Primitives

### Cards & Data Tables
*   **Rule:** Forbid divider lines. 
*   **Execution:** Separate table rows using a alternating background: `surface_container_lowest` for odd rows and `surface_container_low` for even rows. 
*   **Density:** Use `body-sm` (0.75rem) for table content to maximize information density without sacrificing legibility.

### Buttons
*   **Primary:** Solid `primary` (#002660) with `on_primary` (#ffffff) text. Use `DEFAULT` (0.25rem) rounding for a "sharp" professional look.
*   **Secondary:** `surface_container_high` background with `on_surface` text. No border.

### Status Chips (Semantic Precision)
*   **Approved:** `on_primary_container` text on a subtle green tint.
*   **High Risk:** `on_error_container` text on `error_container`.
*   **Design Note:** Chips should be rectangular with `sm` (0.125rem) rounding to maintain the "official document" aesthetic.

### Input Fields
*   Background should be `surface_container_highest` (#e0e3e6). 
*   On focus, transition the background to `surface_container_lowest` (#ffffff) and apply a 1px `primary` Ghost Border.

### Context-Specific Component: The "Audit Trail"
*   A vertical timeline component using `outline` (#747783) for the track. Each node is a tiny `primary` dot. This provides a "legal history" of every food safety action.

---

## 6. Do's and Don'ts

### Do
*   **Use Asymmetry:** In dashboard layouts, use a 70/30 split for the main content and the "Details/Action" panel to create visual interest.
*   **Prioritize Type over Icons:** If an icon isn't immediately obvious, use a `label-sm` text tag. Clarity is more authoritative than "clean" iconography.
*   **Use Surface Tones for Grouping:** Group related form fields by placing them on a shared `surface_container_low` background.

### Don't
*   **Don't Use Pure Black:** Use `on_background` (#191c1e) for text. Pure black is too harsh for high-density reading.
*   **Don't Use 100% Opaque Borders:** This creates "visual noise" that fatigues the regulator's eyes during 8-hour shifts.
*   **Don't Use Vibrant Gradients:** Keep gradients limited to the `primary` range to maintain a "Sober" and "Serious" atmosphere.