## 1. Merge settings pages

- [x] 1.1 Rewrite `settings/page.tsx`: add Tabs (Profile, API Keys, Publishing) using `useTabParam`, move existing Profile and API Keys content into their respective tab panels
- [x] 1.2 Move publishing page content (`settings/publishing/page.tsx`) into the Publishing tab panel inline
- [x] 1.3 Delete `settings/publishing/page.tsx`
- [x] 1.4 Replace all inline messages with sonner toasts
- [x] 1.5 Wrap page in Suspense for Next.js static page + useSearchParams compatibility

## 2. Fix references

- [x] 2.1 Update podcast settings publishing tab: change `/settings/publishing` link to `/settings?tab=publishing`
- [x] 2.2 Verify frontend builds
