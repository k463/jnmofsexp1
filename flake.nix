# jnmofsexp1 - LIC_PRJ_DESC
# Copyright (C) 2025 Elthevoypra
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

{
  inputs = {
    flake-parts.url = "github:hercules-ci/flake-parts";
    nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1.*.tar.gz";
    # default = Darwin+Linux+x86+ARM, or default-linux, default-darwin, etc.
    # see https://github.com/nix-systems/
    systems.url = "github:nix-systems/default";
  };

  outputs = inputs@{ flake-parts, systems, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = (import systems);
      perSystem = { config, self', inputs', pkgs, system, ... }: {
        devShells.default = pkgs.mkShell {
          # packages needed for building the software in this repo
          nativeBuildInputs = with pkgs; [
            gradle_8
            jdk24_headless
          ];
          # packages needed at runtime for running software in this repo
          packages = with pkgs; [
            jdt-language-server
            kotlin-language-server
          ];
        };
      };
    };
}
