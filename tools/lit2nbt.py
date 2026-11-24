import gzip, struct, math, sys
def rd(b,p,t):
    if t==1: return b[p],p+1
    if t==2: return struct.unpack('>h',b[p:p+2])[0],p+2
    if t==3: return struct.unpack('>i',b[p:p+4])[0],p+4
    if t==4: return struct.unpack('>q',b[p:p+8])[0],p+8
    if t==5: return struct.unpack('>f',b[p:p+4])[0],p+4
    if t==6: return struct.unpack('>d',b[p:p+8])[0],p+8
    if t==7:
        n=struct.unpack('>i',b[p:p+4])[0];return b[p+4:p+4+n],p+4+n
    if t==8:
        n=struct.unpack('>H',b[p:p+2])[0];return b[p+2:p+2+n].decode(),p+2+n
    if t==9:
        it=b[p];n=struct.unpack('>i',b[p+1:p+5])[0];p+=5;L=[]
        for _ in range(n):
            v,p=rd(b,p,it);L.append(v)
        return (it,L),p
    if t==10:
        d={}
        while b[p]!=0:
            tt=b[p];n=struct.unpack('>H',b[p+1:p+3])[0];name=b[p+3:p+3+n].decode();p+=3+n
            v,p=rd(b,p,tt);d[name]=(tt,v)
        return d,p+1
    if t==11:
        n=struct.unpack('>i',b[p:p+4])[0];return list(struct.unpack('>%di'%n,b[p+4:p+4+4*n])),p+4+4*n
    if t==12:
        n=struct.unpack('>i',b[p:p+4])[0];return list(struct.unpack('>%dq'%n,b[p+4:p+4+8*n])),p+4+8*n
def s(x): e=x.encode();return struct.pack('>H',len(e))+e
def wr(t,v):
    if t==1: return struct.pack('>b',v)
    if t==3: return struct.pack('>i',v)
    if t==8: return s(v)
    if t==9:
        it,L=v;return bytes([it])+struct.pack('>i',len(L))+b''.join(wr(it,x) for x in L)
    if t==10:
        o=b''
        for k,(tt,vv) in v.items(): o+=bytes([tt])+s(k)+wr(tt,vv)
        return o+b'\0'
src,dst=sys.argv[1],sys.argv[2]
b=gzip.open(src).read()
root,_=rd(b,3+len(b[3:3+0]),10) if False else (None,None)
n=struct.unpack('>H',b[1:3])[0];root,_=rd(b,3+n,10)
reg=list(root['Regions'][1].values())[0][1]
sz=reg['Size'][1];sx,sy,sz_=[abs(sz[k][1]) for k in 'xyz']
pal=reg['BlockStatePalette'][1][1]
bits=max(2,(len(pal)-1).bit_length())
arr=reg['BlockStates'][1]
mask=(1<<bits)-1
def get(i):
    bi=i*bits;w=bi>>6;o=bi&63
    v=(arr[w]&0xFFFFFFFFFFFFFFFF)>>o
    if o+bits>64: v|=(arr[w+1]&0xFFFFFFFFFFFFFFFF)<<(64-o)
    return v&mask
palout=[];air=None
for i,p in enumerate(pal):
    e={'Name':(8,'minecraft:cracked_deepslate_bricks' if p['Name'][1]=='minecraft:spawner' else p['Name'][1])}
    if 'Properties' in p:
        e['Properties']=(10,{k:(8,v[1]) for k,v in p['Properties'][1].items()})
    palout.append(e)
    if p['Name'][1]=='minecraft:air': air=i
blocks=[]
for y in range(sy):
    for z in range(sz_):
        for x in range(sx):
            st=get(y*sx*sz_+z*sx+x)
            if st==air: continue
            blocks.append({'pos':(9,(3,[x,y,z])),'state':(3,st)})
out={'size':(9,(3,[sx,sy,sz_])),'palette':(9,(10,palout)),'blocks':(9,(10,blocks)),'entities':(9,(10,[])),'DataVersion':(3,3465)}
data=b'\n\0\0'+wr(10,out)
gzip.open(dst,'wb').write(data)
print(sx,sy,sz_,len(pal),'palette',len(blocks),'blocks')
print([p['Name'][1] for p in pal])
