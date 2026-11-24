import sys,collections
sys.argv=['x','/mnt/user-data/uploads/18927.litematic','/tmp/m.nbt']
exec(open('tools/lit2nbt.py').read())
solid=set();air=set()
names=[p['Name'][1] for p in pal]
passable={'minecraft:air','minecraft:glow_lichen','minecraft:cobweb','minecraft:redstone_wire','minecraft:carpet','minecraft:black_carpet','minecraft:soul_wall_torch','minecraft:redstone_wall_torch','minecraft:tripwire','minecraft:tripwire_hook','minecraft:lever','minecraft:crimson_button','minecraft:birch_wall_sign','minecraft:crimson_wall_sign'}
grid={}
for y in range(sy):
  for z in range(sz_):
    for x in range(sx):
      grid[(x,y,z)]=names[get(y*sx*sz_+z*sx+x)]
def free(p): return grid.get(p,'minecraft:air') in passable if p in grid else False
def stand(p):
    x,y,z=p
    return free((x,y,z)) and free((x,y+1,z)) and (x,y-1,z) in grid and grid[(x,y-1,z)] not in passable and grid[(x,y-1,z)] not in('minecraft:lava','minecraft:water')
cells=[p for p in grid if stand(p)]
print(len(cells))
# start: standing cell near min corner
start=min(cells,key=lambda p:(p[0]+p[2])+abs(p[1]-1)*3)
dist={start:0};q=collections.deque([start])
while q:
    c=q.popleft()
    for dx,dz in((1,0),(-1,0),(0,1),(0,-1)):
        for dy in(0,1,-1):
            n=(c[0]+dx,c[1]+dy,c[2]+dz)
            if n not in dist and stand(n):
                dist[n]=dist[c]+1;q.append(n)
far=max(dist,key=dist.get)
print('start',start,'reach',len(dist),'exit',far,dist[far])
