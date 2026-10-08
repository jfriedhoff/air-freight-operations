export function createElementWithText(type: string, text: string): HTMLElement {
    const element = document.createElement(type);
    element.appendChild(document.createTextNode(text));
    return element;
}
