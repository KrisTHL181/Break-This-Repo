{
  description = "喵~";

  inputs = {
    nixpkgs.url = "github:Nixos/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
  };

  outputs =
    {
      nixpkgs,
      flake-parts,
      ...
    }@inputs:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "aarch64-darwin"
      ];
      perSystem = { pkgs, ... }: {
        devShells.default = pkgs.mkShellNoCC {
          # packages = with pkgs; [
          # ];
          shellHook = /* bash */ ''
            echo "hello world"
          '';
        };
      };
    };
}
