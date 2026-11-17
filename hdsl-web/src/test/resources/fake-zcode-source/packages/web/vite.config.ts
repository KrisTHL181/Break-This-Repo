// Fake ZCode web sources for ZcodePatchTest / ZcodeBuilderTest: the smallest
// files that still carry every anchor ZcodePatch relies on. When upstream moves
// one of these lines the patch must fail loudly — so a test suite that applies
// the patch to this tree is the honest check, not a copy of the real bundle.
export default defineConfig(() => {
  return {
    plugins: [pdfJsCMapsPlugin(), react(), tailwindcss(), thirdPartyNoticesVitePlugin()],
    resolve: {
      alias: {},
    },
  };
});
