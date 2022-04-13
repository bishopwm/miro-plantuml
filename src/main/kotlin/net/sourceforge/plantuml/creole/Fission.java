/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2023, Arnaud Roques
 *
 * Project Info:  http://plantuml.com
 * 
 * If you like this project or if you find it useful, you can support us at:
 * 
 * http://plantuml.com/patreon (only 1$ per month!)
 * http://plantuml.com/paypal
 * 
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 * 
 *
 */
package net.sourceforge.plantuml.creole;

import net.sourceforge.plantuml.awt.geom.Dimension2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.sourceforge.plantuml.LineBreakStrategy;
import net.sourceforge.plantuml.creole.atom.AbstractAtom;
import net.sourceforge.plantuml.creole.atom.Atom;
import net.sourceforge.plantuml.creole.legacy.AtomText;
import net.sourceforge.plantuml.graphic.StringBounder;
import net.sourceforge.plantuml.ugraphic.UGraphic;

public class Fission {

	private final Stripe stripe;
	private final LineBreakStrategy maxWidth;

	public Fission(Stripe stripe, LineBreakStrategy maxWidth) {
		this.stripe = stripe;
		this.maxWidth = Objects.requireNonNull(maxWidth);
	}

	public List<Stripe> getSplitted(StringBounder stringBounder) {
		final double valueMaxWidth = maxWidth.getMaxWidth();
		if (valueMaxWidth == 0) {
			return Arrays.asList(stripe);
		}
		final List<Stripe> result = new ArrayList<>();
		StripeSimpleInternal current = new StripeSimpleInternal(stripe.getLHeader());
		double remainingSpace = valueMaxWidth;
		for (Atom atom : noHeader()) {
			while (true) {
				final List<Atom> splitInTwo = atom.splitInTwo(stringBounder, remainingSpace);
				final Atom part1 = splitInTwo.get(0);
				final double widthPart1 = part1.calculateDimension(stringBounder).getWidth();
				current.addAtom(part1, widthPart1);
				remainingSpace -= widthPart1;
				if (remainingSpace <= 0) {
					result.add(current);
					current = new StripeSimpleInternal(blank(stripe.getLHeader()));
					remainingSpace = valueMaxWidth;
				}
				if (splitInTwo.size() == 1) {
					break;
				}
				atom = splitInTwo.get(1);
				if (remainingSpace < valueMaxWidth
						&& atom.calculateDimension(stringBounder).getWidth() > remainingSpace) {
					result.add(current);
					current = new StripeSimpleInternal(blank(stripe.getLHeader()));
					remainingSpace = valueMaxWidth;
				}
			}
		}
		if (remainingSpace < valueMaxWidth) {
			result.add(current);
		}
		return Collections.unmodifiableList(result);
	}

	private List<Atom> noHeader() {
		final List<Atom> atoms = stripe.getAtoms();
		if (stripe.getLHeader() == null) {
			return atoms;
		}
		return atoms.subList(1, atoms.size());
	}

	private static Atom blank(final Atom header) {
		if (header == null) {
			return null;
		}
		return new AbstractAtom() {

			public Dimension2D calculateDimension(StringBounder stringBounder) {
				return header.calculateDimension(stringBounder);
			}

			public double getStartingAltitude(StringBounder stringBounder) {
				return header.getStartingAltitude(stringBounder);
			}

			public void drawU(UGraphic ug) {
			}

		};
	}

	private Collection<? extends Atom> getSplitted(StringBounder stringBounder, Atom atom) {
		if (atom instanceof AtomText) {
			return ((AtomText) atom).getSplitted(stringBounder, maxWidth);
		}
		return Collections.singleton(atom);
	}

	static class StripeSimpleInternal implements Stripe {

		private final List<Atom> atoms = new ArrayList<>();
		private double totalWidth;

		private StripeSimpleInternal(Atom header) {
			if (header != null) {
				this.atoms.add(header);
			}
		}

		public List<Atom> getAtoms() {
			return Collections.unmodifiableList(atoms);
		}

		private void addAtom(Atom atom, double width) {
			this.atoms.add(atom);
			this.totalWidth += width;
		}

		public Atom getLHeader() {
			return null;
		}

	}

}
