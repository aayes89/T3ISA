/*
 * The MIT License
 *
 * Copyright 2026 Slam.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package t3os.APPS.Browser.CSS;

/**
 *
 * @author Slam
 */
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import t3os.APPS.Browser.T3HtmlNode;

/**
 * Representa una regla CSS con un selector y declaraciones asociadas.
 */
public class T3CSSRule {

    private static final Pattern SELECTOR_PATTERN = Pattern.compile(
            "^[#.\\w-][#.\\w-]*(?:\\s*[:>~+]?\\s*[#.\\w-\\[\\]=\\*~\\|]+)*$"
    );

    private final String selector;
    private final Map<String, String> declarations;
    private final int specificity;

    /**
     * Crea una nueva regla CSS.
     *
     * @param selector Selector CSS
     * @param declarations Declaraciones CSS (propiedad: valor)
     * @throws IllegalArgumentException si el selector es nulo, vacío o inválido
     */
    public T3CSSRule(String selector, Map<String, String> declarations) {
        Objects.requireNonNull(selector, "El selector no puede ser nulo");
        String cleanedSelector = selector.trim().toLowerCase();
        if (cleanedSelector.isEmpty()) {
            throw new IllegalArgumentException("El selector no puede estar vacío");
        }
        if (!SELECTOR_PATTERN.matcher(cleanedSelector).matches()) {
            throw new IllegalArgumentException("Selector inválido: " + cleanedSelector);
        }
        this.selector = cleanedSelector;
        this.declarations = declarations != null ? Collections.unmodifiableMap(new HashMap<>(declarations)) : Collections.emptyMap();
        this.specificity = calculateSpecificity(cleanedSelector);
    }

    /**
     * Calcula la especificidad del selector según las reglas CSS.
     *
     * @param selector Selector CSS
     * @return Valor de especificidad
     */
    private int calculateSpecificity(String selector) {
        int[] specificity = new int[3]; // [IDs, Clases/Atributos, Elementos]
        String[] parts = selector.split("\\s*[:>~+]?\\s*");

        for (String part : parts) {
            if (part.contains(":")) {
                part = part.substring(0, part.indexOf(":")); // Ignorar pseudo-clases
            }
            if (part.startsWith("#")) {
                specificity[0]++; // ID
            } else if (part.startsWith(".") || part.contains("[")) {
                specificity[1]++; // Clase o atributo
            } else if (!part.isEmpty()) {
                specificity[2]++; // Elemento
            }
        }
        return specificity[0] * 100 + specificity[1] * 10 + specificity[2];
    }

    /**
     * Verifica si la regla coincide con un nodo HTML.
     *
     * @param node Nodo HTML
     * @return true si el selector coincide con el nodo
     */
    public boolean matches(T3HtmlNode node) {
        String[] selectorParts = selector.split("\\s*[:>~+]?\\s*");
        if (selectorParts.length == 0 || selectorParts[0].trim().isEmpty()) {
            return false;
        }

        // Verificar el selector principal
        String mainSelector = selectorParts[0].replaceAll(":.*", "").trim();
        String idAttr = node.getAttributes().getOrDefault("id", "");
        String classAttr = node.getAttributes().getOrDefault("class", "");
        boolean mainMatch;

        if (mainSelector.contains(".")) {
            String[] classParts = mainSelector.split("\\.");
            String base = classParts[0].isEmpty() ? node.getTagName() : classParts[0];
            List<String> classes = classParts.length > 1 ? Arrays.asList(classParts).subList(1, classParts.length) : Collections.emptyList();
            mainMatch = base.equals(node.getTagName()) || (base.startsWith("#") && base.substring(1).equals(idAttr));
            if (!classes.isEmpty()) {
                mainMatch &= Arrays.asList(classAttr.split("\\s+")).containsAll(classes);
            }
        } else if (mainSelector.startsWith("#")) {
            mainMatch = mainSelector.substring(1).equals(idAttr);
        } else if (mainSelector.contains("[")) {
            mainMatch = matchesAttributeSelector(mainSelector, node);
        } else {
            mainMatch = mainSelector.equals(node.getTagName());
        }

        // Soporte básico para pseudo-clases
        if (selector.contains(":nth-child(") && mainMatch) {
            try {
                int index = Integer.parseInt(selector.replaceAll("[^0-9]", ""));
                return node.getParent() != null && node.getParent().getChildIndex(node) == index - 1;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Verificar selectores combinados (por ejemplo, div > p)
        if (selectorParts.length > 1 && mainMatch) {
            String combinator = selector.contains(">") ? ">" : " ";
            return matchesAncestor(node.getParent(), Arrays.asList(selectorParts).subList(1, selectorParts.length), combinator);
        }

        return mainMatch;
    }

    /**
     * Verifica si un selector de atributo coincide con el nodo.
     *
     * @param selector Selector de atributo
     * @param node Nodo HTML
     * @return true si coincide
     */
    private boolean matchesAttributeSelector(String selector, T3HtmlNode node) {
        if (!selector.contains("[") || !selector.contains("]")) {
            return false;
        }
        String attrPart = selector.substring(selector.indexOf("[") + 1, selector.indexOf("]"));
        String[] parts = attrPart.split("=");
        if (parts.length != 2) {
            return false;
        }
        String attrName = parts[0].trim();
        String attrValue = parts[1].replace("\"", "").replace("'", "").trim();
        return node.getAttributes().getOrDefault(attrName, "").equals(attrValue);
    }

    /**
     * Verifica si los selectores restantes coinciden con los ancestros.
     *
     * @param parent Nodo padre
     * @param remainingSelectors Selectores restantes
     * @param combinator Combinador (> o espacio)
     * @return true si coincide
     */
    private boolean matchesAncestor(T3HtmlNode parent, List<String> remainingSelectors, String combinator) {
        if (parent == null || remainingSelectors.isEmpty()) {
            return false;
        }
        String nextSelector = remainingSelectors.get(0).replaceAll(":.*", "").trim();
        boolean parentMatch = false;

        if (nextSelector.contains(".")) {
            String[] classParts = nextSelector.split("\\.");
            String base = classParts[0].isEmpty() ? parent.getTagName() : classParts[0];
            List<String> classes = classParts.length > 1 ? Arrays.asList(classParts).subList(1, classParts.length) : Collections.emptyList();
            parentMatch = base.equals(parent.getTagName());
            if (!classes.isEmpty()) {
                String classAttr = parent.getAttributes().getOrDefault("class", "");
                parentMatch &= Arrays.asList(classAttr.split("\\s+")).containsAll(classes);
            }
        } else if (nextSelector.startsWith("#")) {
            parentMatch = nextSelector.substring(1).equals(parent.getAttributes().getOrDefault("id", ""));
        } else {
            parentMatch = nextSelector.equals(parent.getTagName());
        }

        if (parentMatch && remainingSelectors.size() == 1) {
            return true;
        }

        if (combinator.equals(">")) {
            return parentMatch && matchesAncestor(parent.getParent(), remainingSelectors.subList(1, remainingSelectors.size()), combinator);
        } else {
            T3HtmlNode current = parent;
            while (current != null) {
                if (matchesAncestor(current, remainingSelectors.subList(1, remainingSelectors.size()), combinator)) {
                    return true;
                }
                current = current.getParent();
            }
        }
        return false;
    }

    /**
     * Obtiene el selector CSS.
     *
     * @return Selector
     */
    public String getSelector() {
        return selector;
    }

    /**
     * Obtiene las declaraciones CSS.
     *
     * @return Mapa inmodificable de declaraciones
     */
    public Map<String, String> getDeclarations() {
        return declarations;
    }

    /**
     * Obtiene la especificidad de la regla.
     *
     * @return Valor de especificidad
     */
    public int getSpecificity() {
        return specificity;
    }
}
