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

# workaround for arrterian/nix-env-selector#87 (nix-env-selector does not work with flakes)
# https://github.com/arrterian/nix-env-selector/issues/87
# credit: https://github.com/arrterian/nix-env-selector/issues/87#issuecomment-2079805980

(builtins.getFlake ("git+file://" + toString ./.)).devShells.${builtins.currentSystem}.default
