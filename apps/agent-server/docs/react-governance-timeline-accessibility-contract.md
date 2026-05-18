# React Governance Timeline Accessibility Contract

## 1. Accessibility Goals

The React Governance Timeline panel should be operable, perceivable, and understandable for keyboard and assistive technology users.

The timeline remains a read-only, navigation-only audit surface.

## 2. Keyboard Navigation

Required keyboard-accessible behaviors:

- timeline row navigation must be keyboard accessible
- `NEXT` and `PREVIOUS` pagination controls must be keyboard accessible
- filter interactions must be keyboard accessible
- retry interaction must be keyboard accessible
- reset cursor recovery interaction must be keyboard accessible

## 3. Screen-reader Semantics

The timeline should provide screen-reader friendly semantics for:

- timeline rows
- loading state
- empty state
- error state
- degraded state
- pagination controls

The screen-reader experience should preserve that the surface is informational and read-only.

## 4. Loading, Error, and Degraded Announcements

The following states should be screen-reader perceivable:

- loading initial page
- loading older events
- loading newer events
- invalid cursor recovery
- retry after 5xx
- degraded timeline disclosure
- partial timeline banner
- read-only error state

## 5. Severity Badge Accessibility

Severity and degraded state must not be communicated by color only.

Required principle:

- severity badges should include textual meaning
- degraded state should include textual disclosure

## 6. Timestamp Accessibility

Timestamp rendering should support:

- human-readable presentation
- machine-readable representation

This helps both assistive technology and inspection workflows.

## 7. Focus Management

Focus should remain predictable during timeline interaction.

Recommended principles:

- avoid focus loss after pagination
- avoid unexpected focus reset after refresh
- preserve meaningful focus target after cursor reset or retry

## 8. Pagination Accessibility

Pagination controls and interactions should support:

- keyboard activation
- understandable labels for older and newer event navigation
- non-visual distinction between `NEXT` and `PREVIOUS`

## 9. Read-only Interaction Accessibility

Timeline interaction remains navigation-only.

The accessible interaction model must not imply mutation or execution capability.

## 10. Forbidden Accessibility Anti-patterns

The following must be avoided:

- severity or degraded status communicated by color only
- mouse-only pagination
- screen-reader silent loading or error state
- focus loss after pagination
- approve control
- execute control
- remediate control
- kubectl action
- ArgoCD action
- GitOps action
- RAG action
- Qdrant action

## 11. Non-goals

This contract does not introduce:

- React JSX implementation
- ARIA attribute implementation
- keyboard event handler implementation
- focus trap implementation
- CSS or Tailwind implementation
