import sympy
from sympy import oo

sympy.init_printing()

x = sympy.Symbol('x')

print(sympy.limit(sympy.sin(x) / x, x, oo))

print(sympy.limit((1 + 1 / x) ** x, x, oo))
