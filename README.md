# Vacuum Robot
A simple vacuum robot was made using 4 different agents.
The agents communicate with one another using messages.

# Relation between agents:
- The environment agent resembles a "real life environment", it cannot start messages, only respond to the ones gotten. It communicates with the sensor agent aswell as the actuator.
- The sensor agent attempts to retrieve information from the environment agent in a 3x3 grid around the robot, then passes the information to the Brain.
- The brain agent checks the received information from the sensor and maps out a path for the robot to go to clean up dirty cells. If no dirty cells are found, the sensors ask the environment for a general direction to the closest dirty cell.
- The brain communicates between its sensors and actuators.
- The actuator decrypts the brain's message and executes the actions to the robot such as move and clean.

Pathfinding for the robot in this case is a simple BFS algorithm since the cost is always = 1

![image](https://github.com/user-attachments/assets/71c8904c-2a73-4621-bf2f-da31d7a63dc0)
![image](https://github.com/user-attachments/assets/50f64b2a-f184-4c1b-926b-7fe1b60d0487)
![image](https://github.com/user-attachments/assets/4c74130e-47e3-4881-bee1-4c62e75d7dc4)
![image](https://github.com/user-attachments/assets/e2b8036a-7bf5-4345-a277-b2d8eb51c043)

