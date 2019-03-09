import matplotlib.pyplot as plt
import csv

input_file = "/home/ole/master/test_onto/coords.csv"

x = []
y = []
labels = []

counter = 0

with open(input_file,'r') as csvfile:
    plots = csv.reader(csvfile, delimiter=',')
    for row in plots:
        x.append(float(row[0]))
        y.append(float(row[1]))
        labels.append(str(row[2]))
        counter += 1
        #if counter > 50:
            #break

#plt.scatter(x,y, label='Loaded from file!')
plt.xlabel('x')
plt.ylabel('y')
plt.title('Plotting the vectors!!!!!')
plt.legend()

for i,type in enumerate(labels):
    xi = x[i]
    yi = y[i]
    plt.scatter(xi, yi, marker='o', color='blue')
    plt.text(xi+0.3, yi+0.3, type, fontsize=9)

#fig, ax = plt.subplots()
#for i, txt in enumerate(x):
    #ax.annotate(txt, (x[i], y[i]))

plt.show()
