from matplotlib import pyplot as plt
import numpy as np

x = np.linspace(0, 2*np.pi, 1001)
y = np.sin(x)
plt.plot(x, y)
plt.title('График синусоиды')
plt.xlabel('x')
plt.ylabel('sin(x)')
plt.grid(True)
plt.show()
