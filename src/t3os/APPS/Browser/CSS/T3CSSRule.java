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
import java.util.regex.Matcher;
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
        int ids = 0;
        int classes = 0;
        int elements = 0;

        String cleaned = selector.trim();

        Matcher idMatcher = Pattern.compile("#[\\w-]+").matcher(cleaned);
        while (idMatcher.find()) {
            ids++;
        }

        Matcher classMatcher = Pattern.compile("\\.[\\w-]+|\\[[^\\]]+\\]|:[\\w-]+(?:\\([^)]*\\))?").matcher(cleaned);
        while (classMatcher.find()) {
            classes++;
        }

        String withoutSpecificity = cleaned.replaceAll("#[\\w-]+", " ")
                .replaceAll("\\.[\\w-]+", " ")
                .replaceAll("\\[[^\\]]+\\]", " ")
                .replaceAll(":[\\w-]+(?:\\([^)]*\\))?", " ")
                .replaceAll("[>+~*]", " ");

        Matcher elementMatcher = Pattern.compile("(?<![\\w-])[a-zA-Z][\\w-]*").matcher(withoutSpecificity);
        while (elementMatcher.find()) {
            elements++;
        }

        return ids * 100 + classes * 10 + elements;
    }

    /**
     * Verifica si la regla coincide con un nodo HTML.
     *
     * @param node Nodo HTML
     * @return true si el selector coincide con el nodo
     */
    public boolean matches(T3HtmlNode node) {
        if (node == null || selector == null || selector.trim().isEmpty()) {
            return false;
        }

        String cleaned = selector.trim();

        // Separar los componentes del selector conservando sus combinadores.
        Matcher matcher = Pattern.compile("([^\\s>+~]+)\\s*([>+~]?)\\s*").matcher(cleaned);

        List<String> parts = new java.util.ArrayList<>();
        List<String> combinators = new java.util.ArrayList<>();

        while (matcher.find()) {
            String part = matcher.group(1).trim();
            if (!part.isEmpty()) {
                parts.add(part);
                combinators.add(matcher.group(2));
            }
        }

        if (parts.isEmpty()) {
            return false;
        }

        int last = parts.size() - 1;
        if (!matchesSimpleSelector(parts.get(last), node)) {
            return false;
        }

        T3HtmlNode current = node;

        for (int i = last - 1; i >= 0; i--) {
            String combinator = combinators.get(i);

            if (">".equals(combinator)) {
                current = current.getParent();
                if (current == null || !matchesSimpleSelector(parts.get(i), current)) {
                    return false;
                }
            } else {
                // Descendiente: buscar un ancestro coincidente.
                T3HtmlNode ancestor = current.getParent();
                while (ancestor != null && !matchesSimpleSelector(parts.get(i), ancestor)) {
                    ancestor = ancestor.getParent();
                }

                if (ancestor == null) {
                    return false;
                }
                current = ancestor;
            }
        }

        return true;
    }

    private boolean matchesSimpleSelector(String simple, T3HtmlNode node) {
        if (simple == null || simple.isEmpty() || node == null) {
            return false;
        }

        String tag = node.getTagName();
        Map<String, String> attributes = node.getAttributes();

        Matcher tagMatcher = Pattern.compile("^[a-zA-Z][\\w-]*").matcher(simple);

        if (tagMatcher.find() && !tagMatcher.group().equalsIgnoreCase(tag)) {
            return false;
        }

        Matcher idMatcher = Pattern.compile("#([\\w-]+)").matcher(simple);
        while (idMatcher.find()) {
            if (!idMatcher.group(1).equals(attributes.getOrDefault("id", ""))) {
                return false;
            }
        }

        Matcher classMatcher = Pattern.compile("\\.([\\w-]+)").matcher(simple);

        List<String> nodeClasses = Arrays.asList(
                attributes.getOrDefault("class", "").trim().isEmpty()
                ? new String[0]
                : attributes.get("class").trim().split("\\s+")
        );

        while (classMatcher.find()) {
            if (!nodeClasses.contains(classMatcher.group(1))) {
                return false;
            }
        }

        Matcher attrMatcher = Pattern.compile("\\[([\\w-]+)(?:\\s*=\\s*[\"']?([^\\]\"']+)[\"']?)?\\]").matcher(simple);

        while (attrMatcher.find()) {
            String name = attrMatcher.group(1);
            String expected = attrMatcher.group(2);

            if (!attributes.containsKey(name)) {
                return false;
            }

            if (expected != null && !expected.trim().equals(attributes.get(name))) {
                return false;
            }
        }

        if (simple.contains(":nth-child(")) {
            Matcher nthMatcher = Pattern.compile(":nth-child\\((\\d+)\\)").matcher(simple);

            if (nthMatcher.find()) {
                if (node.getParent() == null) {
                    return false;
                }

                int expectedIndex = Integer.parseInt(nthMatcher.group(1)) - 1;
                if (node.getParent().getChildIndex(node) != expectedIndex) {
                    return false;
                }
            }
        }

        return true;
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
